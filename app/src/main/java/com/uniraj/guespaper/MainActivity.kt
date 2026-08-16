package com.uniraj.guespaper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { UnirajApp() } }
}

@Composable
fun UnirajApp() {
 var token by remember { mutableStateOf<String?>(null) }
 var user by remember { mutableStateOf<User?>(null) }
 var screen by remember { mutableStateOf("home") }
 MaterialTheme {
  if (token == null) AuthScreen(onAuth={t,u->{token=t;user=u}})
  else when(screen) {
   "purchases" -> PurchasesScreen(token!!, onBack={screen="home"})
   "orders" -> OrdersScreen(token!!, onBack={screen="home"})
   else -> HomeScreen(token!!, user!!, onLogout={token=null;user=null}, onPurchases={screen="purchases"}, onOrders={screen="orders"})
  }
 }
}

@Composable
private fun AuthScreen(onAuth:(String,User)->Unit) {
 var register by remember { mutableStateOf(false) }
 var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
 var error by remember { mutableStateOf<String?>(null) }; var busy by remember { mutableStateOf(false) }
 val scope=rememberCoroutineScope()
 Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center) {
  Text("Uniraj Guess Paper",style=MaterialTheme.typography.headlineMedium)
  Text("Rajasthan University • Guess Papers",style=MaterialTheme.typography.bodyMedium)
  Spacer(Modifier.height(24.dp))
  if(register) OutlinedTextField(name,{name=it},label={Text("Name")},modifier=Modifier.fillMaxWidth())
  Spacer(Modifier.height(8.dp)); OutlinedTextField(email,{email=it},label={Text("Email")},modifier=Modifier.fillMaxWidth())
  Spacer(Modifier.height(8.dp)); OutlinedTextField(password,{password=it},label={Text("Password")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
  error?.let { Spacer(Modifier.height(8.dp)); Text(it,color=MaterialTheme.colorScheme.error) }
  Spacer(Modifier.height(16.dp)); Button(enabled=!busy,onClick={scope.launch{busy=true;error=null;try{val r=if(register)ApiProvider.api.register(RegisterRequest(name,email,password)) else ApiProvider.api.login(AuthRequest(email,password));onAuth(r.token,r.user)}catch(e:Exception){error=e.message?:"Request failed"};busy=false}},modifier=Modifier.fillMaxWidth()){Text(if(busy)"Please wait…" else if(register)"Create account" else "Login")}
  TextButton(onClick={register=!register}){Text(if(register)"Already have an account? Login" else "New user? Create account")}
  Text("Payment: UPI / PhonePe • Manual admin approval",style=MaterialTheme.typography.bodySmall)
 }
}

@Composable
private fun HomeScreen(token:String,user:User,onLogout:()->Unit,onPurchases:()->Unit,onOrders:()->Unit) {
 var papers by remember { mutableStateOf<List<Paper>>(emptyList()) }; var error by remember { mutableStateOf<String?>(null) }
 val scope=rememberCoroutineScope(); val context=LocalContext.current
 LaunchedEffect(Unit){try{papers=ApiProvider.api.papers()}catch(e:Exception){error=e.message?:"Unable to load papers"}}
 Scaffold(topBar={TopAppBar(title={Text("Uniraj Guess Paper")},actions={TextButton(onClick=onPurchases){Text("My Papers")};TextButton(onClick=onOrders){Text("Orders")};TextButton(onClick=onLogout){Text("Logout")}})}){pad->
  LazyColumn(Modifier.padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   item{Text("Welcome, ${user.name}",style=MaterialTheme.typography.titleLarge);Text("Choose your Rajasthan University guess paper")}
   error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
   items(papers){paper->PaperCard(paper,token,context,scope)}
  }
 }
}

@Composable
private fun PaperCard(paper:Paper,token:String,context:android.content.Context,scope:kotlinx.coroutines.CoroutineScope){
 var show by remember{mutableStateOf(false)}
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(paper.title,style=MaterialTheme.typography.titleMedium);Text("${paper.subject} • ${paper.semester?:""}");Text("₹${paper.price_paise/100}",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(8.dp));Button(onClick={show=true}){Text("Buy with PhonePe / UPI")}}}
 if(show) PaymentDialog(paper,token,context,scope,onDismiss={show=false})
}

@Composable
private fun PaymentDialog(paper:Paper,token:String,context:android.content.Context,scope:kotlinx.coroutines.CoroutineScope,onDismiss:()->Unit){
 var order by remember{mutableStateOf<OrderResponse?>(null)};var txn by remember{mutableStateOf("")};var msg by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){try{order=ApiProvider.api.createOrder(token,mapOf("paperId" to paper.id))}catch(e:Exception){msg=e.message?:"Could not create order"}}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Pay ₹${paper.price_paise/100}")},text={Column{Text("UPI ID: ${order?.upiId?:"7300349017@ybl"}");Text("Merchant: Nitin Saini");Spacer(Modifier.height(8.dp));Text("1. Tap Pay with PhonePe / UPI.\n2. Pay the exact amount.\n3. Enter your UPI transaction/reference ID below.");Spacer(Modifier.height(8.dp));OutlinedTextField(txn,{txn=it},label={Text("UPI Transaction ID")},modifier=Modifier.fillMaxWidth());msg?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={Button(enabled=order!=null&&!busy,onClick={scope.launch{busy=true;try{val r=ApiProvider.api.submitPayment(token,order!!.orderRef,SubmitPaymentRequest(txn));msg=r["message"]?.toString()?:"Submitted"}catch(e:Exception){msg=e.message?:"Submit failed"};busy=false}}){Text(if(busy)"Submitting…" else "Submit Payment")}},dismissButton={TextButton(onClick={order?.let{launchUpi(context,it.upiId,paper.price_paise/100)}?:Unit}){Text("Open PhonePe")}})
}

private fun launchUpi(context:android.content.Context,upi:String,amount:Int){val uri=Uri.parse("upi://pay?pa=${Uri.encode(upi)}&pn=${Uri.encode("Nitin Saini")}&am=$amount&cu=INR&tn=${Uri.encode("Uniraj Guess Paper")}");try{context.startActivity(Intent(Intent.ACTION_VIEW,uri))}catch{Toast.makeText(context,"No UPI app found",Toast.LENGTH_LONG).show()}}

@Composable
private fun OrdersScreen(token:String,onBack:()->Unit){var orders by remember{mutableStateOf<List<Order>>(emptyList())};LaunchedEffect(Unit){try{orders=ApiProvider.api.orders(token)}catch{}};Column(Modifier.fillMaxSize().padding(16.dp)){TextButton(onClick=onBack){Text("← Back")};Text("My Orders",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(orders){o->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(o.title);Text("₹${o.amount_paise/100} • ${o.status}");Text("UPI Ref: ${o.upi_txn_id?:"Not submitted"}")}}}}}}

@Composable
private fun PurchasesScreen(token:String,onBack:()->Unit){var purchases by remember{mutableStateOf<List<Purchase>>(emptyList())};LaunchedEffect(Unit){try{purchases=ApiProvider.api.purchases(token)}catch{}};Column(Modifier.fillMaxSize().padding(16.dp)){TextButton(onClick=onBack){Text("← Back")};Text("My Papers",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(purchases){p->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(p.title);Text("${p.subject} • Purchased");p.pdfUrl?.let{url->Button(onClick={val i=Intent(Intent.ACTION_VIEW,Uri.parse(url));try{contextStart(i)}catch{}}){Text("Open PDF")}}}}}}}}

private var currentContext:android.content.Context?=null
private fun contextStart(i:Intent){currentContext?.startActivity(i)}
