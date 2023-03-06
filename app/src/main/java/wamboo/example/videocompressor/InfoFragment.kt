package wamboo.example.videocompressor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import wamboo.example.videocompressor.databinding.FragmentInfoBinding

class InfoFragment : Fragment() {
    private lateinit var mAdView2 : AdView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    lateinit var binding: FragmentInfoBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        MobileAds.initialize(requireActivity()) {}
        val adRequest = AdRequest.Builder().build()
        mAdView2 = binding.root.findViewById(R.id.adView2)
        mAdView2.loadAd(adRequest)
        //return inflater.inflate(R.layout.fragment_info, container, false)
        return binding.root
    }

    companion object {
        fun newInstance() = InfoFragment()
    }
}