import re

with open('front/src/views/AdminView.vue', 'r') as f:
    content = f.read()

# Fix the end tags correctly!
# Replace the old `</div> </div> </section> </div>` block for the campusVerifications pane with properly closed tags.
# In the original file, it was:
#           </section>
#         </div>
#       </div>
#       </section>
#     </div>
# But my first script replaced `<div class="admin-page">\n    <section class="admin-shell">\n` with `<div class="admin-page sidebar-layout" ...>\n  <aside ...>...</aside>\n  <main class="admin-main-content">\n`
# So we need to replace the end tags `      </section>\n    </div>` with `    </main>\n  </div>`.
# The first script tried to do this but missed the `</section>` at 913 if there were extra spaces.
# Let's just use regex to replace the last occurrence of `</section>\s*</div>` before `<!-- Membership Modal -->`.

pattern = re.compile(r'</section>\s*</div>\s*</div>\s*</section>\s*</div>\s*<!-- Membership Modal -->', re.MULTILINE)
if pattern.search(content):
    content = pattern.sub(r'</section>\n        </div>\n      </div>\n    </main>\n\n    <!-- Membership Modal -->', content)
else:
    # If the original file was slightly different:
    pattern2 = re.compile(r'</section>\s*</div>\s*</div>\s*</section>\s*<!-- Membership Modal -->', re.MULTILINE)
    if pattern2.search(content):
        content = pattern2.sub(r'</section>\n        </div>\n      </div>\n    </main>\n\n    <!-- Membership Modal -->', content)

with open('front/src/views/AdminView.vue', 'w') as f:
    f.write(content)
