package com.drivecheckcl.ui.components.input

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivecheckcl.theme.ChileBlue
import com.drivecheckcl.theme.InputBackground
import com.drivecheckcl.theme.InputBorder
import com.drivecheckcl.theme.TextHint
import com.drivecheckcl.theme.TextSecondary

/** Campo de texto estándar de la app: label pequeño arriba + OutlinedTextField estilizado. */
@Composable
fun AppTextField(
    value:                String,
    onValueChange:        (String) -> Unit,
    label:                String,
    placeholder:          String,
    leadingIcon:          @Composable (() -> Unit)? = null,
    trailingIcon:         @Composable (() -> Unit)? = null,
    keyboardType:         KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column {
        Text(
            text          = label,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Medium,
            color         = TextSecondary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value                = value,
            onValueChange        = onValueChange,
            placeholder          = { Text(placeholder, fontSize = 13.sp, color = TextHint) },
            leadingIcon          = leadingIcon,
            trailingIcon         = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
            singleLine           = true,
            modifier             = Modifier.fillMaxWidth(),
            shape                = RoundedCornerShape(8.dp),
            colors               = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = InputBackground,
                focusedContainerColor   = InputBackground,
                unfocusedBorderColor    = InputBorder,
                focusedBorderColor      = ChileBlue,
                cursorColor             = ChileBlue
            )
        )
    }
}
