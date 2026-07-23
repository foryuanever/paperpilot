import re

with open('front/src/views/AdminView.vue', 'r') as f:
    content = f.read()

# 1. Add isSidebarCollapsed to data
if 'isSidebarCollapsed: false' not in content:
    content = content.replace('activeTab: \'users\',', "activeTab: 'users',\n      isSidebarCollapsed: false,")

# 2. Extract SVG icons for tabs (if any). Let's use simple lucide icons.
icons = {
    'users': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>',
    'recharges': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2"></rect><line x1="2" y1="10" x2="22" y2="10"></line></svg>',
    'teams': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>',
    'models': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect><rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect><line x1="6" y1="6" x2="6.01" y2="6"></line><line x1="6" y1="18" x2="6.01" y2="18"></line></svg>',
    'logs': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 17 10 11 4 5"></polyline><line x1="12" y1="19" x2="20" y2="19"></line></svg>',
    'forumReports': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>',
    'campusVerifications': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>',
    'messages': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>'
}

tabs = [
    ('users', '用户目录与授权'),
    ('recharges', '充值入账记录'),
    ('teams', '科研团队管理'),
    ('models', 'AI 路由与模型'),
    ('logs', '系统操作日志'),
    ('forumReports', '论坛举报处理'),
    ('campusVerifications', '校园认证审核'),
    ('messages', '站内消息发布')
]

sidebar_html = """
    <aside class="admin-sidebar" :class="{ 'collapsed': isSidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo-box">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
        </div>
        <span class="logo-text" v-show="!isSidebarCollapsed">PaperSolver Admin</span>
        <button class="collapse-btn" @click="isSidebarCollapsed = !isSidebarCollapsed">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6" v-if="!isSidebarCollapsed"></polyline>
            <polyline points="9 18 15 12 9 6" v-else></polyline>
          </svg>
        </button>
      </div>
      
      <nav class="admin-sidebar-nav">
"""
for tab_id, tab_label in tabs:
    sidebar_html += f"""        <button class="sidebar-tab-btn" :class="{{ active: activeTab === '{tab_id}' }}" @click="activeTab = '{tab_id}'">
          <span class="tab-icon">{icons[tab_id]}</span>
          <span class="tab-label" v-show="!isSidebarCollapsed">{tab_label}</span>
        </button>
"""
sidebar_html += """      </nav>
      <div class="sidebar-footer">
        <router-link to="/library" class="back-link" v-show="!isSidebarCollapsed">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="tab-icon"><path d="M15 18l-6-6 6-6"/></svg>
          返回前台文献库
        </router-link>
      </div>
    </aside>
"""

# Replace the top nav with the sidebar layout
# We will wrap the existing content in a main tag.
# Current template structure:
# <template>
#   <div class="admin-page">
#     <section class="admin-shell">
#       <!-- Title & Header -->

# Let's find `<div class="admin-page">` and replace it
new_wrapper = f"""<div class="admin-page sidebar-layout" :class="{{ 'sidebar-collapsed': isSidebarCollapsed }}">
{sidebar_html}
    <main class="admin-main-content">
      <!-- Title & Header -->"""

content = content.replace('<div class="admin-page">\n    <section class="admin-shell">\n      <!-- Title & Header -->', new_wrapper)
content = content.replace('<div class="admin-page">\r\n    <section class="admin-shell">\r\n      <!-- Title & Header -->', new_wrapper)

# We need to remove the top tabs capsule:
#       <!-- Main Layout Panels (Tabbed Layout) -->
#       <div class="admin-main-layout">
#         <!-- Tabs Capsule -->
#         <div class="admin-tabs-nav">...</div>
#         <!-- Tab Content: Users -->
import re
tabs_capsule_pattern = re.compile(r'<!-- Tabs Capsule -->\s*<div class="admin-tabs-nav">.*?</div>\s*<!-- Tab Content:', re.DOTALL)
content = re.sub(tabs_capsule_pattern, '<!-- Tab Content:', content)

# Adjust closing tags
# The old layout had:
#       </div>
#       </section>
#     </div>
# We removed `<section class="admin-shell">` from the opening, so we must remove `</section>` from the end and close `<main>`
content = content.replace('      </section>\n    </div>', '    </main>\n  </div>')
content = content.replace('      </section>\r\n    </div>', '    </main>\r\n  </div>')

# Add CSS for sidebar layout
css_addition = """

.sidebar-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #0b0f19;
}

.admin-sidebar {
  width: 260px;
  background: #111827;
  border-right: 1px solid rgba(255,255,255,0.05);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  z-index: 50;
}
.admin-sidebar.collapsed {
  width: 72px;
}

.sidebar-header {
  height: 72px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  position: relative;
}
.sidebar-collapsed .sidebar-header {
  padding: 0 16px;
  justify-content: center;
}

.logo-box {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 12px;
  flex-shrink: 0;
}
.logo-box svg { width: 20px; height: 20px; }

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: white;
  white-space: nowrap;
}

.collapse-btn {
  position: absolute;
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  background: #1e293b;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 50%;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 60;
}
.collapse-btn:hover {
  background: #334155;
  color: white;
}
.collapse-btn svg { width: 14px; height: 14px; }

.admin-sidebar-nav {
  flex: 1;
  padding: 20px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.sidebar-tab-btn {
  display: flex;
  align-items: center;
  padding: 12px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  width: 100%;
}
.sidebar-tab-btn:hover {
  background: rgba(255,255,255,0.03);
  color: #f8fafc;
}
.sidebar-tab-btn.active {
  background: rgba(99, 102, 241, 0.15);
  color: #818cf8;
}

.tab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-right: 12px;
  flex-shrink: 0;
}
.sidebar-collapsed .tab-icon { margin-right: 0; }
.tab-icon svg { width: 18px; height: 18px; }

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255,255,255,0.05);
}
.back-link {
  display: flex;
  align-items: center;
  color: #64748b;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.2s;
}
.back-link:hover { color: #94a3b8; }
.back-link .tab-icon { width: 16px; height: 16px; margin-right: 8px; }
.back-link .tab-icon svg { width: 14px; height: 14px; }

.admin-main-content {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
  background: transparent;
}
.admin-tabs-nav { display: none !important; }

"""

if '' in content:
    content = content.replace('', css_addition)
else:
    content += css_addition

with open('front/src/views/AdminView.vue', 'w') as f:
    f.write(content)

print("Done structural migration.")
