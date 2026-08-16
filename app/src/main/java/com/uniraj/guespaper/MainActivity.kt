package com.uniraj.guespaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Paper(val id:Int,val title:String,val subject:String,val price:Int)

class MainActivity: ComponentActivity(){
 override fun onCreate(savedInstanceState: Bundle?){ super.onCreate(savedInstanceState); setContent { App() } }
}

@Composable fun App(){
 val papers = remember { listOf(
  Paper(1,"B.Sc. Chemistry Guess Paper","Chemistry",49),
  Paper(2,"B.Sc. Mathematics Guess Paper","Mathematics",49),
  Paper(3,"B.Sc. Physics Guess Paper","Physics",49),
  Paper(4,"B.A. Hindi Guess Paper","Hindi",5)
 ) }
 MaterialTheme { Scaffold(topBar={TopAppBar(title={Text("Uniraj Guess Paper")})}) { p ->
  LazyColumn(contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp), modifier=Modifier.padding(p)) {
   item { Text("Rajasthan University", style=MaterialTheme.typography.headlineSmall); Text("Guess papers • Secure purchase", style=MaterialTheme.typography.bodyMedium) }
   items(papers){ paper -> Card { Column(Modifier.padding(16.dp)) {
    Text(paper.title, style=MaterialTheme.typography.titleMedium); Text(paper.subject)
    Spacer(Modifier.height(8.dp)); Text("₹${paper.price}")
    Spacer(Modifier.height(8.dp)); Button(onClick={}){Text("Buy Paper")}
   } } }
  }
 } }
}
