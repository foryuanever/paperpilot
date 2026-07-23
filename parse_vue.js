const fs = require('fs');
const content = fs.readFileSync('front/src/views/AdminView.vue', 'utf-8');
const template = content.substring(content.indexOf('<template>'), content.indexOf('</template>') + 11);

let stack = [];
const regex = /<\/?([a-zA-Z0-9\-]+)[^>]*>/g;
let match;
const voidElements = new Set(['area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link', 'meta', 'param', 'source', 'track', 'wbr']);

while ((match = regex.exec(template)) !== null) {
    const tagFull = match[0];
    const tagName = match[1];
    
    if (tagFull.endsWith('/>') || voidElements.has(tagName.toLowerCase())) {
        continue; // self-closing or void
    }
    
    if (tagFull.startsWith('</')) {
        if (stack.length === 0) {
            console.log(`Unmatched closing tag: ${tagFull} at index ${match.index}`);
        } else {
            const last = stack.pop();
            if (last.tagName !== tagName) {
                console.log(`Mismatched closing tag: expected </${last.tagName}> but found ${tagFull} at index ${match.index}. Opened at ${last.index}`);
            }
        }
    } else {
        stack.push({ tagName, index: match.index, full: tagFull });
    }
}
if (stack.length > 0) {
    console.log("Unclosed tags remaining:");
    stack.forEach(s => console.log(s.full, 'at index', s.index));
} else {
    console.log("All tags balanced.");
}
