package wamboo.eco.videocompressor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import wamboo.eco.videocompressor.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    /// list of tab title
    private var tabTitles = arrayOf("Home", "Information")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        /* adapter for viewpager to display tabs */
        val adapter = ViewPagerAdapter(this)

        /// set adapter to viewpager
        binding.viewPager.adapter = adapter

        /* attach viewpager with tabLayout to update its label when page change
        *  assign tab labels
        *  change page on tab click
        * */
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        /// add tabLayout click listener to switch tab by clicking
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                /// change tab
                binding.viewPager.currentItem = tab?.position ?: 0
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }
        })


        /// set custom setting in actionbar
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.actionbar_title)
    }

    override fun onStop() {
        super.onStop()


    }
}