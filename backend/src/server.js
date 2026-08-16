import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import pg from 'pg';
import crypto from 'crypto';

dotenv.config();
const { Pool } = pg;
const app = express();
app.use(cors());
app.use(express.json({limit:'1mb'}));
const pool = process.env.DATABASE_URL ? new Pool({connectionString:process.env.DATABASE_URL,ssl:process.env.NODE_ENV==='production'?{rejectUnauthorized:false}:false}) : null;
const JWT_SECRET = process.env.JWT_SECRET || 'change-me-in-production';
const UPI_ID = process.env.UPI_ID || '7300349017@ybl';
const sign = u => jwt.sign({id:u.id,email:u.email,role:u.role}, JWT_SECRET, {expiresIn:'30d'});
const auth = async (req,res,next) => { try { const t=(req.headers.authorization||'').replace('Bearer ',''); if(!t) return res.status(401).json({error:'Login required'}); req.user=jwt.verify(t,JWT_SECRET); next(); } catch { res.status(401).json({error:'Invalid session'}); } };
const admin = (req,res,next) => req.user?.role==='admin' ? next() : res.status(403).json({error:'Admin access required'});
const fallback=[
 {id:1,title:'B.Sc. Chemistry Guess Paper',subject:'Chemistry',semester:'1st Semester',price_paise:4900},
 {id:2,title:'B.Sc. Mathematics Guess Paper',subject:'Mathematics',semester:'1st Semester',price_paise:4900},
 {id:3,title:'B.Sc. Physics Guess Paper',subject:'Physics',semester:'1st Semester',price_paise:4900},
 {id:4,title:'B.A. Hindi Guess Paper',subject:'Hindi',semester:'1st Semester',price_paise:500}
];
app.get('/health',(req,res)=>res.json({ok:true,service:'uniraj-guess-paper-api',database:!!pool,upi:UPI_ID}));
app.get('/api/config',(req,res)=>res.json({upiId:UPI_ID,merchantName:'Uniraj Guess Paper'}));
app.get('/api/papers',async(req,res)=>{if(!pool)return res.json(fallback);try{const r=await pool.query('SELECT id,title,subject,semester,price_paise FROM papers WHERE active=true ORDER BY id');res.json(r.rows);}catch{res.status(500).json({error:'Unable to load papers'});}});
app.post('/api/auth/register',async(req,res)=>{if(!pool)return res.status(503).json({error:'Database is not configured'});const {name,email,password}=req.body;if(!name||!email||!password||password.length<6)return res.status(400).json({error:'Name, email and 6+ character password required'});try{const hash=await bcrypt.hash(password,12);const r=await pool.query('INSERT INTO users(name,email,password_hash) VALUES($1,$2,$3) RETURNING id,name,email,role',[name,email.toLowerCase(),hash]);res.status(201).json({user:r.rows[0],token:sign(r.rows[0])});}catch{res.status(409).json({error:'Email already registered'});}});
app.post('/api/auth/login',async(req,res)=>{if(!pool)return res.status(503).json({error:'Database is not configured'});try{const r=await pool.query('SELECT id,name,email,password_hash,role FROM users WHERE email=$1',[String(req.body.email||'').toLowerCase()]);if(!r.rowCount||!(await bcrypt.compare(req.body.password||'',r.rows[0].password_hash)))return res.status(401).json({error:'Invalid email or password'});const {password_hash,...user}=r.rows[0];res.json({user,token:sign(user)});}catch{res.status(500).json({error:'Login failed'});}});
app.post('/api/orders',auth,async(req,res)=>{const paperId=Number(req.body.paperId);let paper;if(pool){const r=await pool.query('SELECT * FROM papers WHERE id=$1 AND active=true',[paperId]);paper=r.rows[0];}else paper=fallback.find(p=>p.id===paperId);if(!paper)return res.status(404).json({error:'Paper not found'});const ref='UGP-'+Date.now()+'-'+crypto.randomBytes(3).toString('hex').toUpperCase();try{if(pool)await pool.query('INSERT INTO orders(user_id,paper_id,razorpay_order_id,amount_paise,status,payment_ref) VALUES($1,$2,$3,$4,\'pending\',$3)',[req.user.id,paperId,ref,paper.price_paise]);res.json({orderRef:ref,paperId,amount:paper.price_paise,upiId:UPI_ID,status:'pending'});}catch{res.status(500).json({error:'Unable to create order'});}});
app.post('/api/orders/:orderRef/submit-payment',auth,async(req,res)=>{const txn=String(req.body.upiTxnId||'').trim();if(!txn)return res.status(400).json({error:'UPI transaction/reference ID required'});if(!pool)return res.json({submitted:true,status:'pending'});try{const r=await pool.query('UPDATE orders SET upi_txn_id=$1 WHERE payment_ref=$2 AND user_id=$3 AND status=\'pending\' RETURNING id',[txn,req.params.orderRef,req.user.id]);if(!r.rowCount)return res.status(404).json({error:'Order not found or already processed'});res.json({submitted:true,status:'pending',message:'Payment submitted for admin approval'});}catch(e){res.status(409).json({error:'Transaction ID already submitted'});}});
app.get('/api/me/orders',auth,async(req,res)=>{if(!pool)return res.json([]);const r=await pool.query('SELECT o.id,o.payment_ref,o.upi_txn_id,o.amount_paise,o.status,o.created_at,p.title,p.subject FROM orders o JOIN papers p ON p.id=o.paper_id WHERE o.user_id=$1 ORDER BY o.created_at DESC',[req.user.id]);res.json(r.rows);});
app.get('/api/me/purchases',auth,async(req,res)=>{if(!pool)return res.json([]);const r=await pool.query('SELECT p.id,p.title,p.subject,p.semester,p.price_paise,p.pdf_url,pu.purchased_at FROM purchases pu JOIN papers p ON p.id=pu.paper_id WHERE pu.user_id=$1 ORDER BY pu.purchased_at DESC',[req.user.id]);res.json(r.rows.map(x=>({...x,pdfUrl:x.pdf_url})));});
app.get('/api/papers/:id/access',auth,async(req,res)=>{if(!pool)return res.status(503).json({error:'Database is not configured'});const r=await pool.query('SELECT p.id,p.title,p.pdf_url FROM purchases pu JOIN papers p ON p.id=pu.paper_id WHERE pu.user_id=$1 AND p.id=$2',[req.user.id,Number(req.params.id)]);if(!r.rowCount)return res.status(403).json({error:'Paper not purchased or not approved'});if(!r.rows[0].pdf_url)return res.status(404).json({error:'PDF is not uploaded yet'});res.json({id:r.rows[0].id,title:r.rows[0].title,pdfUrl:r.rows[0].pdf_url});});
app.get('/api/admin/orders',auth,admin,async(req,res)=>{if(!pool)return res.json([]);const r=await pool.query('SELECT o.id,o.payment_ref,o.upi_txn_id,o.amount_paise,o.status,o.created_at,u.name,u.email,p.title FROM orders o JOIN users u ON u.id=o.user_id JOIN papers p ON p.id=o.paper_id WHERE o.status=\'pending\' ORDER BY o.created_at ASC');res.json(r.rows);});
app.post('/api/admin/orders/:id/approve',auth,admin,async(req,res)=>{if(!pool)return res.status(503).json({error:'Database is not configured'});const client=await pool.connect();try{await client.query('BEGIN');const r=await client.query('SELECT * FROM orders WHERE id=$1 FOR UPDATE',[Number(req.params.id)]);if(!r.rowCount||r.rows[0].status!=='pending')throw new Error('Order not pending');const o=r.rows[0];if(!o.upi_txn_id)throw new Error('Customer has not submitted UPI transaction ID');await client.query('UPDATE orders SET status=\'paid\',approved_at=now() WHERE id=$1',[o.id]);await client.query('INSERT INTO purchases(user_id,paper_id,razorpay_payment_id,payment_ref) VALUES($1,$2,$3,$4) ON CONFLICT(user_id,paper_id) DO NOTHING',[o.user_id,o.paper_id,o.upi_txn_id,o.upi_txn_id]);await client.query('COMMIT');res.json({approved:true});}catch(e){await client.query('ROLLBACK');res.status(400).json({error:e.message});}finally{client.release();}});
app.post('/api/admin/orders/:id/reject',auth,admin,async(req,res)=>{if(!pool)return res.status(503).json({error:'Database is not configured'});await pool.query('UPDATE orders SET status=\'rejected\' WHERE id=$1 AND status=\'pending\'',[Number(req.params.id)]);res.json({rejected:true});});
const port=process.env.PORT||8080;app.listen(port,()=>console.log(`API listening on ${port}`));
