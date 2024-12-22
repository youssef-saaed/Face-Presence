package com.yousefsaid04.facepresence.views

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yousefsaid04.facepresence.R
import com.yousefsaid04.facepresence.camerasaver.CameraCollector
import com.yousefsaid04.facepresence.db.AppDatabase
import com.yousefsaid04.facepresence.db.Student
import com.yousefsaid04.facepresence.utils.OUTPUTSIZE
import java.util.concurrent.Executors

val executor = Executors.newSingleThreadExecutor()

@Composable
fun StudentListScreen(modifier: Modifier = Modifier, db: AppDatabase?) {
    val showDialog = remember { mutableStateOf(false) }
    val students = remember { mutableStateOf(listOf<Student>()) }

    if (db != null) {
        students.value = db.studentDao().getAll()
    }

    Column(
        modifier = modifier.background(Color.White)
    ) {
        StatusBar()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            StudentListScrollable(
                modifier = Modifier.fillMaxSize(),
                students = students.value,
                db = db,
                onDelete = { updatedList -> students.value = updatedList }
            )
            AddButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                onClick = { showDialog.value = true }
            )
        }
    }
    if (showDialog.value) {
        AddDialog(
            onDismiss = { showDialog.value = false },
            db = db,
            onAdd = { newStudent ->
                students.value = students.value + newStudent
            }
        )
    }
}

@Composable
fun StudentListScrollable(
    modifier: Modifier,
    students: List<Student>,
    db: AppDatabase?,
    onDelete: (List<Student>) -> Unit
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(students) { student ->
            StudentCard(
                student = student,
                db = db,
                onDelete = { deletedStudent ->
                    onDelete(students.filter { it.id != deletedStudent.id })
                }
            )
        }
    }
}

@Composable
fun StudentCard(student: Student, db: AppDatabase?, onDelete: (Student) -> Unit) {
    val deletePic = painterResource(R.drawable.delete)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.hsl(0F, 0F, 0.95F))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Button(
                onClick = {
                    db?.studentDao()?.deleteStudent(student)
                    onDelete(student)
                },
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .align(Alignment.CenterEnd),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Image(
                    deletePic,
                    contentDescription = "Delete Student: ${student.name}",
                    colorFilter = ColorFilter.lighting(Color.Black, Color.Red)
                )
            }
            Column (modifier = Modifier.align(Alignment.CenterStart)){
                Text(
                    text = student.name + " " + student.id,
                )
                if (student.attended)
                {
                    Text(
                        text = "attended",
                        color = Color.Green,
                    )
                }
                else {
                    Text(
                        text = "Not attended",
                        color = Color.Red,
                    )
                }
            }
        }
    }
}

@Composable
fun AddDialog(onDismiss: () -> Unit, db: AppDatabase?, onAdd: (Student) -> Unit) {
    var name = remember { mutableStateOf("") }
    var studentId = remember { mutableStateOf("") }
    var embedding = remember { mutableStateOf(FloatArray(OUTPUTSIZE)) }
    var centroid = remember { mutableStateOf(FloatArray(OUTPUTSIZE)) }
    var cameraActive = remember { mutableStateOf(false) }
    var captureEnabled = remember { mutableStateOf(true) }
    var pose = remember { mutableStateOf(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)) }
    var n = remember { mutableStateOf(1) }

    val spaceModifier = Modifier.padding(10.dp)
    val TextFieldStyle = TextFieldDefaults.colors(
        unfocusedContainerColor = Color.hsl(0F, 0F, 0.95F),
        focusedContainerColor = Color.hsl(0F, 0F, 0.95F),
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.DarkGray,
        focusedLabelColor = Color.hsl(270F, 1F, 0.5F),
        unfocusedLabelColor = Color.hsl(270F, 1F, 0.85F),
        focusedIndicatorColor = Color.hsl(270F, 1F, 0.5F),
        unfocusedIndicatorColor = Color.hsl(270F, 1F, 0.85F),
        cursorColor = Color.hsl(270F, 1F, 0.5F)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp)
            ) {
                CameraCollector(cameraActive.value) { emb, face ->
                    embedding.value = emb
                    pose.value = face
                    n.value++
                }
                Column {
                    Text(
                        stringResource(R.string.add_student),
                        fontWeight = FontWeight.Bold,
                        modifier = spaceModifier
                    )

                    TextField(
                        value = name.value,
                        onValueChange = { name.value = it },
                        label = { Text(stringResource(R.string.name)) },
                        colors = TextFieldStyle,
                        modifier = spaceModifier
                    )

                    TextField(
                        value = studentId.value,
                        onValueChange = { newText -> studentId.value = newText.trimStart { it == '0' } },
                        label = { Text(stringResource(R.string.student_id)) },
                        colors = TextFieldStyle,
                        modifier = spaceModifier
                    )
                    Image(pose.value.asImageBitmap(), contentDescription = stringResource(R.string.last_pose), modifier = spaceModifier.align(Alignment.CenterHorizontally))
                    TextButton(
                        enabled = captureEnabled.value,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.hsl(124F,.38F, .45F), contentColor = Color.White, disabledContainerColor = Color.hsl(124F,.38F, .85F), disabledContentColor = Color.White),
                        onClick = {
                            executor.execute {
                                captureEnabled.value = false
                                cameraActive.value = true
                                var i = 0
                                var last_emb = 0
                                var cent = FloatArray(OUTPUTSIZE){0F}
                                while (i < 10) {
                                    if (n.value != last_emb)
                                    {
                                        i++;
                                        last_emb = n.value
                                        for (i in 0 until OUTPUTSIZE){
                                            cent[i] += embedding.value[i] / 10
                                        }
                                    }
                                }

                                cameraActive.value = false
                                captureEnabled.value = true
                                centroid.value = cent
                                Log.d("CentroidCapture", "${cameraActive}")
                            }
                        },
                        modifier = spaceModifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.capture))
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = spaceModifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = {
                                name.value = ""
                                studentId.value = ""
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(contentColor = Color.hsl(270F, 1F, 0.5F), containerColor = Color.Transparent)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                val newStudent = Student(
                                    id = studentId.value.toInt(),
                                    name = name.value,
                                    embeddings = centroid.value.joinToString(","),
                                    attended = false
                                )
                                Log.d("StudentAdd", "Adding student with id: ${newStudent.id}, name: ${newStudent.name}, embeddings: ${newStudent.embeddings}")
                                db?.studentDao()?.addStudent(student = newStudent)
                                onAdd(newStudent)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(contentColor = Color.hsl(270F, 1F, 0.5F), containerColor = Color.Transparent)
                        ) {
                            Text(stringResource(R.string.add))
                        }
                    }
                }
            }
        }
    }
}


@Preview()
@Composable
fun StudentListPreview()
{
    StudentListScreen(modifier = Modifier.fillMaxSize(), null)
}

@Composable
fun StatusBar()
{
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp)
            .background(Color.hsl(270F, 1F, 0.5F))
            .padding(
                top = 35.dp,
                bottom = 10.dp,
                end = 10.dp,
                start = 10.dp
            )
    ) {
        Text(
            text = stringResource(R.string.student_list),
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable
fun AddButton(modifier: Modifier = Modifier, onClick: ()->Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.hsl(270F, 1F, 0.5F),
            contentColor = Color.White
        ),
        modifier = modifier
            .width(60.dp)
            .height(60.dp)
    ) {
        Text(
            text = "+",
            fontSize = 20.sp
        )
    }
}
