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
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main dashboard hosting the bottom navigation bar and the main fragment container.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        private const val CONTAINER_ID = 1001
        const val NAV_ID = 1002
    }

    lateinit var bottomNav: BottomNavigationView
        private set

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
            id = CONTAINER_ID
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        bottomNav = BottomNavigationView(this).apply {
            id = NAV_ID
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
            add(0, 1, 0, "Home").setIcon(R.drawable.ic_nav_home)
            add(0, 2, 1, "Learn").setIcon(R.drawable.ic_nav_learn)
            add(0, 3, 2, "Practice").setIcon(R.drawable.ic_nav_practice)
            add(0, 4, 3, "Store").setIcon(R.drawable.ic_nav_store)
            add(0, 5, 4, "Profile").setIcon(R.drawable.ic_nav_profile)
        }

        rootLayout.addView(fragmentContainer)
        rootLayout.addView(bottomNav)
        setContentView(rootLayout)

        // Set navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            val tag = when (item.itemId) {
                1 -> "Home"
                2 -> "Lessons"
                3 -> "Review"
                4 -> "Shop"
                5 -> "Profile"
                else -> "Home"
            }
            switchFragment(tag)
            true
        }

        // Default initial tab load
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = 1
        }

        // Handle back press: navigate to Home tab first, then exit
        onBackPressedDispatcher.addCallback(this) {
            val currentTag = supportFragmentManager.findFragmentById(CONTAINER_ID)?.tag
            if (currentTag != "Home" && bottomNav.selectedItemId != 1) {
                bottomNav.selectedItemId = 1
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun switchFragment(tag: String) {
        val fm = supportFragmentManager
        val current = fm.findFragmentById(CONTAINER_ID)
        if (current != null && current.tag == tag) return

        fm.commit {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            // Hide the currently active fragment if exists
            current?.let { hide(it) }

            // Find or instantiate target fragment
            var target = fm.findFragmentByTag(tag)
            if (target == null) {
                target = when (tag) {
                    "Home" -> HomeFragment()
                    "Lessons" -> LessonsFragment()
                    "Review" -> ReviewFragment()
                    "Shop" -> ShopFragment()
                    "Profile" -> ProfileFragment()
                    else -> HomeFragment()
                }
                add(CONTAINER_ID, target, tag)
            } else {
                show(target)
            }
        }
    }
}
