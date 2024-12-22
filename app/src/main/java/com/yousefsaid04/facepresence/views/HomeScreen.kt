package com.yousefsaid04.facepresence.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yousefsaid04.facepresence.FacePresenceApp
import com.yousefsaid04.facepresence.R
import com.yousefsaid04.facepresence.cameramain.CameraScreen
import com.yousefsaid04.facepresence.db.AppDatabase

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController?, db: AppDatabase) {
    var recording by remember { mutableStateOf(false) }

    val color: Color
    val text: String
    if (recording) {
        color = Color.hsl(359F, 0.59F, 0.38F)
        text = stringResource(R.string.stop)
    }
    else
    {
        color = Color.hsl(124F,.38F, .45F)
        text = stringResource(R.string.start)
    }

    val groupIconPainter = painterResource(R.drawable.group_icon)

    Box(
        modifier = modifier
    ){
        CameraScreen(active = recording, db = db)
        Button(
            onClick = {recording = !recording},
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(30.dp)
                .height(50.dp)
        ) {
            Text(
                text = text
            )
        }
        Button(
            onClick = {
                navController?.navigate(FacePresenceApp.StudentsList.name)
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.hsl(270F,1F, .5F)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(30.dp)
                .height(50.dp)
        )
        {
            Image(
                painter = groupIconPainter,
                colorFilter = ColorFilter.lighting(Color.White, Color.White),
                contentDescription = "Go to student management icon",
                modifier = Modifier.width(30.dp)
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun MainPreview() {
//    FacePresenceTheme {
//        HomeScreen(
//            modifier = Modifier.fillMaxSize(),
//            null
//        )
//    }
//}