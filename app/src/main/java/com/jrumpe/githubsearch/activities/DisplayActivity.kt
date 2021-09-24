package com.jrumpe.githubsearch.activities

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.jrumpe.githubsearch.R
import com.jrumpe.githubsearch.adapters.DisplayAdapter
import com.jrumpe.githubsearch.app.Constants
import com.jrumpe.githubsearch.extensions.showErrorMessage
import com.jrumpe.githubsearch.extensions.toArrayList
import com.jrumpe.githubsearch.extensions.toast
import com.jrumpe.githubsearch.models.Repository
import com.jrumpe.githubsearch.models.SearchResponse
import com.jrumpe.githubsearch.retrofit.GithubAPIService
import com.jrumpe.githubsearch.retrofit.RetrofitClient
import io.realm.*
import io.realm.kotlin.delete
import kotlinx.android.synthetic.main.activity_display.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.header.view.*
import kotlinx.android.synthetic.main.layout_delete_dialog.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DisplayActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var displayAdapter: DisplayAdapter
    private var browsedRepositories: List<Repository> = mutableListOf()
    private var bkmArrList: ArrayList<Repository> = browsedRepositories.toArrayList()
    private val githubAPIService: GithubAPIService by lazy {
        RetrofitClient.githubAPIService
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        setAppUserName()
        setSupportActionBar(toolbar)

        supportActionBar!!.title = "Showing Browsed Results"
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager

        navigationView.setNavigationItemSelectedListener(this)
        val drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        val intent = intent
        if (intent.getIntExtra(Constants.KEY_QUERY_TYPE, -1) == Constants.SEARCH_BY_REPO) {
            val queryRepo = intent.getStringExtra(Constants.KEY_REPO_SEARCH)
            val repoLanguage = intent.getStringExtra(Constants.KEY_LANGUAGE)
            fetchRepositories(queryRepo!!, repoLanguage!!)
        } else {
            val githubUser = intent.getStringExtra(Constants.KEY_GITHUB_USER)
            fetchUserRepositories(githubUser!!)
        }
    }

    private fun setAppUserName() {
        val sp = getSharedPreferences(Constants.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val personName = sp.getString(Constants.KEY_PERSON_NAME, "User")
        val headerView = navigationView.getHeaderView(0)
        headerView.txvName.text = personName
    }

    private fun fetchUserRepositories(githubUser: String) {
        githubAPIService.searchRepositoriesByUser(githubUser)
            .enqueue(object : Callback<List<Repository>> {
                override fun onResponse(
                    call: Call<List<Repository>>,
                    response: Response<List<Repository>>,
                ) {

                    if (response.isSuccessful) {
                        Log.i(TAG, "Post loaded from API $response")
                        response.body()?.let {
                            browsedRepositories = it
                        }

                        if (browsedRepositories.isNotEmpty()) {
                            setupRecyclerView(browsedRepositories)
                        } else {
                            toast("no items found")
                        }
                    } else {
                        Log.i(TAG, "Error " + response)
                        showErrorMessage(response.errorBody()!!)
                    }
                }

                override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                    toast("Error fetching results")
//                    showMessage(this@DisplayActivity, t.message?: "Error fetching results")  // elvis operator
                }
            })
    }

    private fun consumeResponse(response: Response<Repository>): Boolean {
        if (response.isSuccessful) {
            return if (toString().isNotEmpty()) {
                setupRecyclerView(browsedRepositories)
                true
            } else {
                toast("no items found")
                false
            }
        }
        showErrorMessage(response.errorBody()!!)
        return false
    }

    private fun fetchRepositories(queryRep: String, repoLanguage: String) {
        var queryRepo = queryRep
        val query: MutableMap<String, String> = HashMap()
        if (repoLanguage.isNotEmpty()) queryRepo += " language:$repoLanguage"
        query["q"] = queryRepo
        githubAPIService.searchRepositories(query).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(
                call: Call<SearchResponse>,
                response: Response<SearchResponse>,
            ) {
                if (response.isSuccessful) {
                    Log.i(TAG, "posts loaded from API $response")
                    response.body().let {
                        browsedRepositories = response.body()!!.items!!
                    }
                    if (browsedRepositories.isNotEmpty()) {
                        setupRecyclerView(browsedRepositories)
                    } else
                        toast("No Items Found")
                } else {
                    Log.i(TAG, "error $response")
                    showErrorMessage(response.errorBody()!!)
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                Log.i(TAG, t.toString())
                toast(t.toString())
//                showMessage(this@DisplayActivity, t.toString())
            }
        })
    }

    private fun setupRecyclerView(items: List<Repository>) {
        displayAdapter = DisplayAdapter(this@DisplayActivity, items)
        recyclerView.adapter = displayAdapter
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true
        when (menuItem.itemId) {
            R.id.item_bookmark -> {
                consumeMenuEvent({ showBookmarks() }, "Showing Bookmarks")
            }
            R.id.item_browsed_results -> {
                consumeMenuEvent({ showBrowsedResults() }, "Showing Browsed Results")
            }
        }
        return true
    }

    private inline fun consumeMenuEvent(myFunc: () -> Unit, tittle: String) {
        myFunc()
        closeDrawer()
        supportActionBar!!.title = tittle
    }

    private fun showBrowsedResults() {
        displayAdapter.swap(browsedRepositories)
    }

    private fun showBookmarks() {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction(fun(realm: Realm) {
            val bookmarkedRepoList = realm.where(Repository::class.java).findAll()
            displayAdapter.swap(bookmarkedRepoList)

            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    targetViewHolder: RecyclerView.ViewHolder,
                ): Boolean {
                    // Called when the item is dragged.
                    val fromPosition = viewHolder.adapterPosition
                    val toPosition = targetViewHolder.adapterPosition

                    Collections.swap(bookmarkedRepoList, fromPosition, toPosition)

                    recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)

                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // Called when the item is swiped.
                    val position = viewHolder.adapterPosition
                    val deletedBookmark: Repository? = bookmarkedRepoList[position]

                    deleteBookmark(position)
                    updateBookmarksList(deletedBookmark)

                    Snackbar.make(recyclerView, "Deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO") {
                            undoDelete(position, deletedBookmark)
                            updateBookmarksList(deletedBookmark)
                        }
                        .show()
                }
            })

            itemTouchHelper.attachToRecyclerView(recyclerView)

        })

    }

    private fun undoDelete(position: Int, deletedBookmark: Repository?) {
        val bmkList = browsedRepositories.toArrayList().add(position, deletedBookmark!!)

        displayAdapter.notifyItemInserted(position)
        displayAdapter.notifyItemRangeChanged(position, bkmArrList.size)
    }

    private fun updateBookmarksList(deletedBookmark: Repository?) {
        val bmkList = browsedRepositories.toArrayList()
        val position = bmkList.indexOf(deletedBookmark)
        displayAdapter.notifyItemRemoved(position)
        displayAdapter.notifyItemRangeChanged(position, bkmArrList.size)
    }

    private fun deleteBookmark(position: Int) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction(fun(realm: Realm) {
            val bookmarkedRepoList = realm.where(Repository::class.java).findAll()
            bookmarkedRepoList[position]?.deleteFromRealm()
            displayAdapter.swap(bookmarkedRepoList)
            displayAdapter.notifyItemRemoved(position)
            displayAdapter.notifyItemRangeChanged(position, bookmarkedRepoList.size)
//                realm.commitTransaction()
        })

//        bkmArrList.remove(browsedRepositories[position])
//        displayAdapter.notifyItemRemoved(position)
//        displayAdapter.notifyItemRangeChanged(position, bkmArrList.size)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.delete_bookmarks_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_delete -> deleteBookmarks()
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun deleteBookmarks(): Boolean {
        showDialog()
        return true
    }

    private fun showDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.layout_delete_dialog)
        val body = dialog.findViewById(R.id.body) as TextView
        body.text = "Do you want delete bookmarks?"
        val yesBtn = dialog.findViewById(R.id.yesBtn) as Button
        val noBtn = dialog.findViewById(R.id.noBtn) as TextView
        yesBtn.setOnClickListener {
            val realm = Realm.getDefaultInstance()
            realm.executeTransaction(fun(realm: Realm) {
                realm.deleteAll()
                val bookmarkedRepoList = realm.where(Repository::class.java).findAll()
                displayAdapter.swap(bookmarkedRepoList)
                dialog.dismiss()
//                realm.commitTransaction()
            })
        }
        noBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDialog(title: String) {


    }

    private fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) closeDrawer() else {
            super.onBackPressed()
        }
    }

    companion object {
        private val TAG = DisplayActivity::class.java.simpleName
    }
}