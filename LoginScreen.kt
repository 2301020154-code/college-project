package com.example.trackback

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackback.ui.theme.LightGray
import com.example.trackback.ui.theme.LightText
import com.example.trackback.ui.theme.TrackBackTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isEmailError by remember { mutableStateOf(false) }
    var isPasswordError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = Firebase.auth

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            isLoading = true
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
                auth.signInWithCredential(credential).addOnCompleteListener {
                    isLoading = false
                    if (it.isSuccessful) {
                        Toast.makeText(context, "Google Sign-In Successful", Toast.LENGTH_SHORT).show()
                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                    } else {
                        Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                isLoading = false
                Toast.makeText(context, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).padding(8.dp))
                Text("FIND & REUNITE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Connect with your college community", style = MaterialTheme.typography.bodyMedium, color = LightText)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Welcome Back!", style = MaterialTheme.typography.displaySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; isEmailError = false },
                    label = { Text("College Email") },
                    isError = isEmailError,
                    supportingText = { if (isEmailError) Text("Email cannot be empty", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = LightGray,
                        unfocusedContainerColor = LightGray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; isPasswordError = false },
                    label = { Text("Password") },
                    isError = isPasswordError,
                    supportingText = { if (isPasswordError) Text("Password cannot be empty", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, "Toggle password visibility")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = LightGray,
                        unfocusedContainerColor = LightGray
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            isEmailError = true
                            Toast.makeText(context, "Please enter your email to reset password", Toast.LENGTH_SHORT).show()
                        } else {
                            auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener { Toast.makeText(context, "Password reset email sent.", Toast.LENGTH_LONG).show() }
                                .addOnFailureListener { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?", color = LightText)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        isEmailError = email.isBlank()
                        isPasswordError = password.isBlank()
                        if (!isEmailError && !isPasswordError) {
                            isLoading = true
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                    } else {
                                        Toast.makeText(context, task.exception?.message ?: "Login Failed", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LOG IN", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("OR", color = LightText)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut().addOnCompleteListener {
                                launcher.launch(googleSignInClient.signInIntent)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue with Google")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account?", color = LightText)
                TextButton(onClick = { navController.navigate("signup") }) {
                    Text("Get Started")
                }
            }
        }
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    TrackBackTheme {
        LoginScreen(navController = rememberNavController())
    }
}