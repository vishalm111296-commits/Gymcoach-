    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    // JVM-side JSON parsing so unit tests can validate the real seed assets
    // (org.json inside android.jar is stubbed on the unit-test classpath).
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
}
