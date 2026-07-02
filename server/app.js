// server/app.js
require('dotenv').config();
const express = require('express');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const mysql = require('mysql2/promise');

const app = express();
app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// MySQL pool
const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'paperslover',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
});

// Multer storage for file uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, path.join(__dirname, 'uploads'));
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname);
    const id = uuidv4();
    cb(null, `${id}${ext}`);
  },
});
const upload = multer({ storage });

// ---------- Helper functions ----------
async function query(sql, params) {
  const [rows] = await pool.execute(sql, params);
  return rows;
}

// ---------- Routes ----------
// 1. File upload (used by tasks, announcements, resources)
app.post('/api/files', upload.single('file'), async (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'No file uploaded' });
  const filePath = `/uploads/${req.file.filename}`; // URL path
  res.json({
    id: path.parse(req.file.filename).name,
    name: req.file.originalname,
    size: `${(req.file.size / 1024).toFixed(1)} KB`,
    type: req.file.mimetype,
    url: filePath,
  });
});

// 2. File download (direct static serve via /uploads, but provide explicit route)
app.get('/api/files/:id', async (req, res) => {
  // Find file by uuid in uploads folder
  const files = await fs.promises.readdir(path.join(__dirname, 'uploads'));
  const file = files.find(f => path.parse(f).name === req.params.id);
  if (!file) return res.status(404).json({ error: 'File not found' });
  const fullPath = path.join(__dirname, 'uploads', file);
  res.sendFile(fullPath);
});

// 3. Members CRUD (simplified for demo)
app.get('/api/members', async (req, res) => {
  const rows = await query('SELECT * FROM members');
  res.json(rows);
});
app.post('/api/members', async (req, res) => {
  const { name, email, role, tokenLimit } = req.body;
  const id = uuidv4();
  await query('INSERT INTO members (id, name, email, role, tokenLimit, registerTime) VALUES (?,?,?,?,?,NOW())', [id, name, email, role, tokenLimit || 1000000]);
  const member = await query('SELECT * FROM members WHERE id = ?', [id]);
  res.json(member[0]);
});
// other member routes (update, delete) can be added similarly

// 4. Tasks CRUD
app.get('/api/tasks', async (req, res) => {
  const tasks = await query('SELECT * FROM tasks');
  const attachments = await query('SELECT * FROM task_attachments');
  const result = tasks.map(t => ({
    ...t,
    attachments: attachments.filter(a => a.taskId === t.id),
  }));
  res.json(result);
});
app.post('/api/tasks', async (req, res) => {
  const { title, description, deadline, creatorId, attachments } = req.body;
  const id = uuidv4();
  await query('INSERT INTO tasks (id, title, description, deadline, creatorId) VALUES (?,?,?,?,?)', [id, title, description, deadline, creatorId]);
  // save attachments if any
  if (Array.isArray(attachments)) {
    for (const att of attachments) {
      const attId = uuidv4();
      await query('INSERT INTO task_attachments (id, taskId, name, size, type, filePath) VALUES (?,?,?,?,?,?)', [attId, id, att.name, att.size, att.type, att.url]);
    }
  }
  const task = await query('SELECT * FROM tasks WHERE id = ?', [id]);
  const attRows = await query('SELECT * FROM task_attachments WHERE taskId = ?', [id]);
  res.json({ ...task[0], attachments: attRows });
});
app.delete('/api/tasks/:id', async (req, res) => {
  const { id } = req.params;
  await query('DELETE FROM task_attachments WHERE taskId = ?', [id]);
  await query('DELETE FROM tasks WHERE id = ?', [id]);
  res.json({ success: true });
});
// Update status route
app.patch('/api/tasks/:id/status', async (req, res) => {
  const { id } = req.params;
  const { status } = req.body;
  await query('UPDATE tasks SET status = ? WHERE id = ?', [status, id]);
  res.json({ success: true });
});

// 5. Announcements CRUD (with optional image and attachments)
app.get('/api/announcements', async (req, res) => {
  const anns = await query('SELECT * FROM announcements');
  const att = await query('SELECT * FROM announcement_attachments');
  const result = anns.map(a => ({
    ...a,
    attachments: att.filter(x => x.announcementId === a.id),
  }));
  res.json(result);
});
app.post('/api/announcements', async (req, res) => {
  const { title, content, image, link, creatorId, attachments } = req.body;
  const id = uuidv4();
  await query('INSERT INTO announcements (id, title, content, imagePath, link, creatorId, publishTime) VALUES (?,?,?,?,?,NOW())', [id, title, content, image?.url || null, link || null, creatorId]);
  if (Array.isArray(attachments)) {
    for (const a of attachments) {
      const attId = uuidv4();
      await query('INSERT INTO announcement_attachments (id, announcementId, name, size, type, filePath) VALUES (?,?,?,?,?,?)', [attId, id, a.name, a.size, a.type, a.url]);
    }
  }
  const ann = await query('SELECT * FROM announcements WHERE id = ?', [id]);
  const attRows = await query('SELECT * FROM announcement_attachments WHERE announcementId = ?', [id]);
  res.json({ ...ann[0], attachments: attRows });
});
app.delete('/api/announcements/:id', async (req, res) => {
  const { id } = req.params;
  await query('DELETE FROM announcement_attachments WHERE announcementId = ?', [id]);
  await query('DELETE FROM announcements WHERE id = ?', [id]);
  res.json({ success: true });
});

// 6. Resources CRUD
app.get('/api/resources', async (req, res) => {
  const rows = await query('SELECT * FROM resources');
  res.json(rows);
});
app.post('/api/resources', async (req, res) => {
  const { name, size, type, uploaderId, url } = req.body;
  const id = uuidv4();
  await query('INSERT INTO resources (id, name, size, type, uploaderId, uploadTime, filePath) VALUES (?,?,?,?,NOW(),?)', [id, name, size, type, uploaderId, url]);
  const row = await query('SELECT * FROM resources WHERE id = ?', [id]);
  res.json(row[0]);
});
app.delete('/api/resources/:id', async (req, res) => {
  const { id } = req.params;
  await query('DELETE FROM resources WHERE id = ?', [id]);
  res.json({ success: true });
});

// 7. Simple state endpoint to fetch all data for initial load
app.get('/api/state', async (req, res) => {
  const members = await query('SELECT * FROM members');
  const tasks = await query('SELECT * FROM tasks');
  const taskAtt = await query('SELECT * FROM task_attachments');
  const announcements = await query('SELECT * FROM announcements');
  const annAtt = await query('SELECT * FROM announcement_attachments');
  const resources = await query('SELECT * FROM resources');
  const checkins = []; // optional, you can create a table if needed
  res.json({ members, tasks: tasks.map(t => ({ ...t, attachments: taskAtt.filter(a => a.taskId === t.id) })), announcements: announcements.map(a => ({ ...a, attachments: annAtt.filter(at => at.announcementId === a.id) })), resources, checkins });
});

// 8. Auth placeholder (simple email lookup, no password for demo)
app.post('/api/auth/login', async (req, res) => {
  const { email } = req.body;
  const users = await query('SELECT * FROM members WHERE email = ?', [email]);
  if (!users.length) return res.status(404).json({ error: 'User not found' });
  const user = users[0];
  res.json({ id: user.id, name: user.name, email: user.email, role: user.role, tokenLimit: user.tokenLimit, tokenUsed: user.tokenUsed });
});

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => console.log(`Server running on http://localhost:${PORT}`));
