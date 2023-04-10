package wamboo.example.videocompressor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import android.os.PowerManager								   
import androidx.appcompat.app.ActionBar
import androidx.activity.viewModels										   
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint											
import wamboo.example.videocompressor.databinding.ActivityMainBinding
import wamboo.example.videocompressor.vm.CompressViewModel														  

@AndroidEntryPoint				  
class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var hometab = getString(R.string.title_home)
        var infotab = getString(R.string.title_information)
		var historytab = getString(R.string.title_history)
        var tabTitles = arrayOf(hometab, infotab, historytab)
        setContentView(binding.root)

        /* adapter for viewpager to display tabs */
        val adapter = ViewPagerAdapter(this)

        /// set adapter to viewpager
        binding.viewPager.adapter = adapter
		//binding.viewPager.isUserInputEnabled = false

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

	/*override fun onStop() {
        super.onStop()


    }*/
}