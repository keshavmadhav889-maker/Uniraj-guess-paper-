import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import Razorpay from 'razorpay';
import crypto from 'crypto';
dotenv.config();
const app=express(); app.use(cors()); app.use(express.json());
const papers=[
 {id:1,title:'B.Sc. Chemistry Guess Paper',subject:'Chemistry',price:49},
 {id:2,title:'B.Sc. Mathematics Guess Paper',subject:'Mathematics',price:49},
 {id:3,title:'B.Sc. Physics Guess Paper',subject:'Physics',price:49},
 {id:4,title:'B.A. Hindi Guess Paper',subject:'Hindi',price:5}
];
app.get('/health',(req,res)=>res.json({ok:true,service:'uniraj-guess-paper-api'}));
app.get('/api/papers',(req,res)=>res.json(papers));
app.post('/api/orders',async(req,res)=>{
 const paper=papers.find(p=>p.id===Number(req.body.paperId)); if(!paper) return res.status(404).json({error:'Paper not found'});
 if(!process.env.RAZORPAY_KEY_ID||!process.env.RAZORPAY_KEY_SECRET) return res.status(503).json({error:'Payment gateway is not configured'});
 const razorpay=new Razorpay({key_id:process.env.RAZORPAY_KEY_ID,key_secret:process.env.RAZORPAY_KEY_SECRET});
 try { const order=await razorpay.orders.create({amount:paper.price*100,currency:'INR',receipt:`paper_${paper.id}_${Date.now()}`}); res.json({orderId:order.id,keyId:process.env.RAZORPAY_KEY_ID,amount:order.amount,currency:order.currency,paperId:paper.id}); }
 catch(e){res.status(500).json({error:'Unable to create order'});}
});
app.post('/api/payments/verify',(req,res)=>{
 const {razorpay_order_id,razorpay_payment_id,razorpay_signature}=req.body;
 if(!process.env.RAZORPAY_KEY_SECRET) return res.status(503).json({error:'Payment gateway is not configured'});
 const expected=crypto.createHmac('sha256',process.env.RAZORPAY_KEY_SECRET).update(`${razorpay_order_id}|${razorpay_payment_id}`).digest('hex');
 if(!crypto.timingSafeEqual(Buffer.from(expected),Buffer.from(razorpay_signature||''))) return res.status(400).json({verified:false,error:'Invalid payment signature'});
 res.json({verified:true,message:'Payment verified'});
});
const port=process.env.PORT||8080; app.listen(port,()=>console.log(`API listening on ${port}`));
