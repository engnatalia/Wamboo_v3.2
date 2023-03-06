package wamboo.example.videocompressor

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


/// This class is used to inflate fragment as a tab by viewpager
class ViewPagerAdapter(fragmentManager: FragmentActivity) : FragmentStateAdapter(fragmentManager) {

    /// below function used to tell total number to tabs displyaed in viewpager
    override fun getItemCount(): Int {
        return 2
    }

    /// return specific fragment based on index.
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment.newInstance()
            else -> InfoFragment.newInstance()
        }
    }
}