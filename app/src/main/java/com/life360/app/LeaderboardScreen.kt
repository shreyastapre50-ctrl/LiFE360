package com.life360.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val green = Color(0xFF176B45)
    var board by remember { mutableStateOf("Global") }
    var period by remember { mutableStateOf("Weekly") }
    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var groups by remember { mutableStateOf<List<CommunityGroup>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<CommunityGroup?>(null) }
    var groupName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Loading leaderboard…") }

    fun refresh() {
        scope.launch {
            loading = true
            try {
                val start = if (period == "Weekly") LocalDate.now(ZoneOffset.UTC)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay().toInstant(ZoneOffset.UTC).toString() else null
                if (board == "Global") {
                    entries = LeaderboardRepository.global(start)
                } else {
                    groups = LeaderboardRepository.groups()
                    val group = selectedGroup ?: groups.firstOrNull()
                    selectedGroup = group
                    entries = if (group == null) emptyList() else LeaderboardRepository.group(group.id, start)
                }
                message = if (entries.isEmpty()) "No points have been awarded in this period yet." else ""
            } catch (e: Exception) {
                message = "Could not load leaderboard. Apply the latest Supabase migration and check your connection. (${e.message ?: "network error"})"
            } finally { loading = false }
        }
    }

    LaunchedEffect(board, period, selectedGroup?.id) { refresh() }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Spacer(Modifier.width(8.dp))
            Text("Points & Leaderboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = green)
        }
        Text("Turn everyday sustainable choices into progress.", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Global", "Community").forEach { item ->
                FilterChip(selected = board == item, onClick = { board = item }, label = { Text(item) })
            }
            Spacer(Modifier.weight(1f))
            listOf("Weekly", "All-time").forEach { item ->
                FilterChip(selected = period == item, onClick = { period = item }, label = { Text(item) })
            }
        }
        if (board == "Community") {
            Spacer(Modifier.height(8.dp))
            if (groups.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(selectedGroup?.name ?: "Choose community") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        groups.forEach { g -> DropdownMenuItem(text = { Text(g.name) }, onClick = { selectedGroup = g; expanded = false }) }
                    }
                }
                selectedGroup?.let { Text("Invite code: ${it.inviteCode}", fontSize = 12.sp, color = Color.Gray) }
            }
            OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("New college/community name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = {
                scope.launch { try { val created = LeaderboardRepository.createGroup(groupName.trim()); groupName = ""; message = "Community created. Share invite code: ${created.inviteCode}"; groups = LeaderboardRepository.groups(); selectedGroup = created; refresh() } catch (e: Exception) { message = "Create failed: ${e.message}" } }
            }, enabled = groupName.trim().length >= 2) { Text("Create community") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = inviteCode, onValueChange = { inviteCode = it }, label = { Text("Invite code") }, singleLine = true, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    scope.launch { try { LeaderboardRepository.joinGroup(inviteCode); inviteCode = ""; groups = LeaderboardRepository.groups(); selectedGroup = groups.firstOrNull(); message = "Joined community"; refresh() } catch (e: Exception) { message = "Join failed: ${e.message}" } }
                }, enabled = inviteCode.isNotBlank()) { Text("Join") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5EC))) {
            Column(Modifier.padding(16.dp)) {
                Text("Your sustainability score", fontWeight = FontWeight.SemiBold, color = green)
                Text("Points are earned from verified sustainable actions and challenges.", fontSize = 13.sp, color = Color.DarkGray)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (message.isNotBlank()) Text(message, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { entry ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${entry.rank}", fontWeight = FontWeight.Bold, color = green, fontSize = 18.sp, modifier = Modifier.width(52.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.displayName, fontWeight = FontWeight.SemiBold)
                            Text("LiFE360 member", color = Color.Gray, fontSize = 11.sp)
                        }
                        Text("${entry.totalPoints} pts", color = green, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Only points awarded by trusted backend checks appear here. Carbon estimates and points may be unavailable until activity verification is connected.", fontSize = 11.sp, color = Color.Gray)
    }
}
