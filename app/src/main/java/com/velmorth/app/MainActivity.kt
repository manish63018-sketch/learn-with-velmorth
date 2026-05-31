package com.velmorth.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.velmorth.app.ui.home.HomeFragment
import com.velmorth.app.ui.lessons.LessonsFragment
import com.velmorth.app.ui.review.ReviewFragment
import com.velmorth.app.ui.shop.ShopFragment
import com.velmorth.app.ui.profile.ProfileFragment

/**
 * Main dashboard hosting the bottom navigation bar and the main fragment container.
 */
class MainActivity : FragmentActivity() {

    private val containerId = View.generateViewId()
    private val navId = View.generateViewId()
    
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create root layout programmatically to eliminate XML requirements
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFFF8F5EE.toInt()) // Background cream
        }

        val fragmentContainer = FrameLayout(this).apply {
            id = containerId
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        bottomNav = BottomNavigationView(this).apply {
            id = navId
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Premium styling parameters
            setBackgroundColor(0xFFFFFFFF.toInt())
            itemRippleColor = ColorStateList.valueOf(0xFFB7E4C7.toInt())
            itemIconTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_selected),
                    intArrayOf(-android.R.attr.state_selected)
                ),
                intArrayOf(
                    0xFF2D6A4F.toInt(), // Selected Green
                    0xFF6B7280.toInt()  // Muted Grey
                )
            )
            itemTextColor = itemIconTintList
        }

        // Add tabs programmatically
        bottomNav.menu.apply {
            add(0, 1, 0, "Home").setIcon(android.R.drawable.ic_menu_today)
            add(0, 2, 1, "Lessons").setIcon(android.R.drawable.ic_menu_compass)
            add(0, 3, 2, "Review").setIcon(android.R.drawable.ic_menu_edit)
            add(0, 4, 3, "Shop").setIcon(android.R.drawable.ic_menu_slideshow)
            add(0, 5, 4, "Profile").setIcon(android.R.drawable.ic_menu_myplaces)
        }

        rootLayout.addView(fragmentContainer)
        rootLayout.addView(bottomNav)
        setContentView(rootLayout)

        // Set navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                1 -> HomeFragment()
                2 -> LessonsFragment()
                3 -> ReviewFragment()
                4 -> ShopFragment()
                5 -> ProfileFragment()
                else -> HomeFragment()
            }
            switchFragment(selectedFragment, item.title.toString())
            true
        }

        // Default initial tab load
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = 1
        }

        // Handle back press: navigate to Home tab first, then exit
        onBackPressedDispatcher.addCallback(this) {
            val currentTag = supportFragmentManager.findFragmentById(containerId)?.tag
            if (currentTag != "Home" && bottomNav.selectedItemId != 1) {
                bottomNav.selectedItemId = 1
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun switchFragment(fragment: Fragment, tag: String) {
        val current = supportFragmentManager.findFragmentById(containerId)
        if (current != null && current::class.java == fragment::class.java) return

        supportFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            replace(containerId, fragment, tag)
            // Add to back stack for non-home tabs
            if (tag != "Home") {
                addToBackStack(tag)
            }
        }
    }
}
