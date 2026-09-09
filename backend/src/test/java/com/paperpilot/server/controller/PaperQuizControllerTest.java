package com.paperpilot.server.controller;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.paperpilot.server.entity.*;
import com.paperpilot.server.repository.*;
import com.paperpilot.server.service.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PaperQuizControllerTest {
    final ObjectMapper mapper=new ObjectMapper();
    final PaperQuizRepository quizzes=mock(PaperQuizRepository.class);
    final PaperRepository papers=mock(PaperRepository.class);
    final AppUserRepository users=mock(AppUserRepository.class);
    final ModelConfigRepository models=mock(ModelConfigRepository.class);
    final CurrentUserService current=mock(CurrentUserService.class);
    final AiChatService ai=mock(AiChatService.class);
    final AiUsageService usage=mock(AiUsageService.class);
    final String id=UUID.randomUUID().toString();
    final String source="This paper reports an experiment comparing two imaging probes and their fluorescence response. ".repeat(15);
    PaperQuizController controller;
    ArrayNode questions;
    PaperQuizEntity saved;

    @BeforeEach void setup() throws Exception {
        controller=new PaperQuizController(quizzes,papers,users,models,current,ai,usage,mapper);
        when(current.getOrCreateDefaultUserId()).thenReturn(1L);
        PaperEntity paper=new PaperEntity();paper.setUserId(1L);paper.setTitle("Probe study");
        when(papers.findByWorkspaceId("paper")).thenReturn(Optional.of(paper));
        AppUserEntity user=new AppUserEntity();user.setFruitScore(5);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.spendQuizPoints(1L,2)).thenReturn(1);
        ModelConfigEntity model=new ModelConfigEntity();model.setApiKey("test");model.setModelName("test");model.setBaseUrl("http://localhost");
        when(models.findAllBySceneOrderByActiveDescUpdatedAtDesc("paper_quiz")).thenReturn(List.of(model));
        questions=mapper.createArrayNode();
        for(int i=0;i<17;i++) {
            ObjectNode q=questions.addObject();q.put("id",""+(i+1));q.put("type",i<10?"choice":i<15?"boolean":"short");
            q.put("prompt","Question "+i);q.put("answer",i<10?"A":i<15?"true":"Response");
            q.put("explanation","From the experiment");q.put("rubric","Compare probes");
            q.put("sourceId","p1");q.put("quote","This paper reports an experiment");
            q.putArray("options").add("one").add("two").add("three").add("four");
        }
        when(ai.chatJsonWithModelFallbackUnmeteredValidatedForScene(anyString(),anyString(),anyInt(),anyList(),eq("paper_quiz"),any()))
            .thenAnswer(invocation->new AiChatService.ChatResult("test",mapper.createObjectNode().set("questions",questions).toString()));
        when(quizzes.saveAndFlush(any())).thenAnswer(invocation->{saved=invocation.getArgument(0);return saved;});
        when(quizzes.findById(id)).thenAnswer(invocation->Optional.ofNullable(saved));
    }
    JsonNode request() {
        ObjectNode body=mapper.createObjectNode();body.put("requestId",id);body.put("workspaceId","paper");
        body.putArray("paragraphs").addObject().put("id","p1").put("text",source);
        return body;
    }
    @Test void hidesAnswersAndChargesOnlyOnce() throws Exception {
        JsonNode result=controller.generate(request());
        assertEquals(17,result.path("questions").size());assertFalse(result.path("questions").get(0).has("answer"));
        when(quizzes.existsById(id)).thenReturn(true);
        controller.generate(request());verify(users,times(1)).spendQuizPoints(1L,2);
    }
    @Test void rejectsInventedCitationWithoutCharging() {
        ((ObjectNode)questions.get(0)).put("quote","This quotation does not appear in the paper");
        assertThrows(ResponseStatusException.class,()->controller.generate(request()));
        verify(users,never()).spendQuizPoints(anyLong(),anyInt());
    }
    @Test void missingModelDoesNotCharge() {
        when(models.findAllBySceneOrderByActiveDescUpdatedAtDesc("paper_quiz")).thenReturn(List.of());
        assertThrows(ResponseStatusException.class,()->controller.generate(request()));
        verifyNoInteractions(ai);verify(users,never()).spendQuizPoints(anyLong(),anyInt());
    }
    @Test void gradesOnServerAndRepeatedSubmissionDoesNotCharge() throws Exception {
        controller.generate(request());
        when(ai.chatJsonWithModelFallbackUnmeteredValidatedForScene(anyString(),anyString(),anyInt(),anyList(),eq("paper_quiz"),any()))
            .thenReturn(new AiChatService.ChatResult("test","{\"grades\":[{\"id\":\"16\",\"score\":10,\"feedback\":\"Partial evidence\"},{\"id\":\"17\",\"score\":15,\"feedback\":\"Complete\"}]}"));
        ObjectNode body=mapper.createObjectNode();ObjectNode answers=body.putObject("answers");
        for(int i=1;i<=17;i++) answers.put(""+i,i<=10?"A":i<=15?"true":"Student answer");
        body.put("score",100);
        JsonNode result=controller.submit(id,body);
        assertEquals(95,result.path("result").path("score").asInt());
        controller.submit(id,body);
        verify(users,times(1)).spendQuizPoints(1L,2);
    }
    @Test void deniesOtherUsers() throws Exception {
        controller.generate(request());when(current.getOrCreateDefaultUserId()).thenReturn(2L);
        assertThrows(ResponseStatusException.class,()->controller.submit(id,mapper.createObjectNode()));
    }
}
