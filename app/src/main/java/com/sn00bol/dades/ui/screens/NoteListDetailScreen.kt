package com.sn00bol.dades.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Màn hình danh sách và chi tiết ghi chú.
 * Tự động hiển thị 1 hoặc 2 cột tùy theo kích thước màn hình.
 */
@Composable
fun NoteListDetailScreen() {
    val navigator = rememberListDetailPaneScaffoldNavigator<Int>()

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            NoteListPane(onNoteClick = { id ->
                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
            })
        },
        detailPane = {
            val noteId = navigator.currentDestination?.content
            NoteDetailPane(noteId)
        }
    )
}

@Composable
fun NoteListPane(onNoteClick: (Int) -> Unit) {
    val dummyNotes = List(10) { it }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                "Ghi chú của tôi",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(dummyNotes) { id ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onNoteClick(id) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tiêu đề ghi chú #$id", style = MaterialTheme.typography.titleMedium)
                    Text("Nội dung xem trước của ghi chú này...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun NoteDetailPane(id: Int?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        if (id != null) {
            Text("Chi tiết ghi chú #$id", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Đây là nội dung đầy đủ của ghi chú. Dữ liệu này sẽ được giải mã " +
                "từ Database thông qua Repository khi bạn tích hợp logic chính thức.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            Text("Chọn một ghi chú để xem chi tiết", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
