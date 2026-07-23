import re
with open('front/src/views/AdminView.vue', 'r') as f:
    content = f.read()

# Let's just find the closing script tag and insert the styles right after it, wrapped in <style scoped>
# Since the file is messed up (missing <style scoped>), let's fix it.
# Wait, the file currently has raw CSS starting with `.sidebar-layout {` right after `</script>`.

pattern = re.compile(r'</script>\s*\.sidebar-layout \{')
if pattern.search(content):
    content = pattern.sub(r'</script>\n\n<style scoped>\n.sidebar-layout {', content)

with open('front/src/views/AdminView.vue', 'w') as f:
    f.write(content)
