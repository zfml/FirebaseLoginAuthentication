package com.example.firebaseloginauthentication

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.firebaseloginauthentication.presentation.sign_in.GoogleAuthUiClient
import com.example.firebaseloginauthentication.presentation.sign_in.SignInScreen
import com.example.firebaseloginauthentication.presentation.sign_in.SignInViewModel
import com.example.firebaseloginauthentication.profile.ProfileScreen
import com.example.firebaseloginauthentication.ui.theme.FirebaseLoginAuthenticationTheme
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FirebaseLoginAuthenticationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "sign_in",
                    ) {
                        composable("sign_in"){
                          val viewModel = viewModel<SignInViewModel>()
                          val state by viewModel.state.collectAsState()


                          LaunchedEffect(key1 = Unit) {
                              if(googleAuthUiClient.getSignInUser() != null) {
                                  navController.navigate("profile")
                              }
                          }

                          val launcher = rememberLauncherForActivityResult(
                              contract = ActivityResultContracts.StartIntentSenderForResult(),
                              onResult = {result ->
                                  if(result.resultCode == RESULT_OK) {
                                      lifecycleScope.launch {
                                          val signInResult = googleAuthUiClient.signInWithIntent(
                                              intent = result.data ?: return@launch
                                          )
                                          viewModel.onSignInResult(signInResult)
                                      }
                                  }
                              }
                          )

                            LaunchedEffect(key1 = state.isSignInSuccessful) {

                                if(state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign In Successful",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    navController.navigate("profile")
                                    viewModel.resetState()
                                }

                            }

                            SignInScreen(
                                state = state,
                                onSignInClicked = {
                                    Log.d("Clicked","Click")
                                    lifecycleScope.launch {
                                        val signInIntentSender = googleAuthUiClient.signIn()
                                        Log.d("Clicked",signInIntentSender.toString())
                                        launcher.launch(
                                            IntentSenderRequest.Builder(
                                                signInIntentSender ?: return@launch
                                            ).build()
                                        )

                                    }

                                }
                            )

                        }

                        composable(
                            route = "profile"
                        ){
                            ProfileScreen(
                                userData = googleAuthUiClient.getSignInUser(),
                                onSignOut = {
                                    lifecycleScope.launch {
                                        googleAuthUiClient.signOut()

                                        Toast.makeText(
                                            applicationContext,
                                            "Sign Out",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        navController.popBackStack()

                                    }
                                }

                            )

                        }

                    }


                }
            }
        }
    }
}

