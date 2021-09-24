package com.jrumpe.githubsearch.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jrumpe.githubsearch.R
import com.jrumpe.githubsearch.app.Constants
import com.jrumpe.githubsearch.app.MyApplication
import com.jrumpe.githubsearch.extensions.isNotEmpty
import com.jrumpe.githubsearch.extensions.toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MyApplication()
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                toast("Granted")
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                toast("Permission Required")
            }
            else -> {
                toast("Please Grant Permissions")
            }
        }
        reqPersmissions()
        setSupportActionBar(toolbar)

    }

    private fun reqPersmissions(): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                toast("Granted")
                return true
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                toast("Permission Required")
                return false
            }
            else -> {
                toast("Please Grant Permissions")
                return false
            }
        }
    }

    /** Save app username in SharedPreferences  */
    fun saveName(view: View) {
        if (etName.isNotEmpty(inputLayoutName)) {
            val personName = etName.text.toString().trim()
            val sp = getSharedPreferences(Constants.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val editor = sp.edit()
            editor.putString(Constants.KEY_PERSON_NAME, personName)
            editor.apply()
        }
    }

    /** Search repositories on github  after passing data to Display Activity */
    fun listRepositories(view: View) {
        if (etRepoName.isNotEmpty(inputLayoutRepoName)) {
            val queryRepo = etRepoName.text.toString().trim()
            val repoLanguage = etLanguage.text.toString()
            val intent = Intent(
                this@MainActivity,
                DisplayActivity::class.java
            )  //uses Kotlin Reflection(::) to get next activity
            intent.putExtra(Constants.KEY_QUERY_TYPE, Constants.SEARCH_BY_REPO)
            intent.putExtra(Constants.KEY_REPO_SEARCH, queryRepo)
            intent.putExtra(Constants.KEY_LANGUAGE, repoLanguage)
            startActivity(intent)
        }
    }

    /** Search repositories of a particular github user  */
    fun listUserRepositories(view: View) {
        if (etGithubUser.isNotEmpty(inputLayoutGithubUser)) {
            val githubUser = etGithubUser.text.toString()
            val intent = Intent(
                this@MainActivity,
                DisplayActivity::class.java
            )  //uses Kotlin Reflection(::) to get next activity
            intent.putExtra(Constants.KEY_QUERY_TYPE, Constants.SEARCH_BY_USER)
            intent.putExtra(Constants.KEY_GITHUB_USER, githubUser)
            startActivity(intent)
        }
    }

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }
}