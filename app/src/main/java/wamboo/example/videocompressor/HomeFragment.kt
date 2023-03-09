package wamboo.example.videocompressor

import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.work.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.*
import wamboo.example.videocompressor.databinding.FragmentHomeBinding
import wamboo.example.videocompressor.workers.ForegroundWorker
import wamboo.example.videocompressor.workers.VideoCompressionWorker


@Suppress("DEPRECATION")
class HomeFragment : Fragment() {
    private lateinit var mAdView: AdView
    private var videoUrl: Uri? = null
    private var compressedFilePath = ""
    lateinit var pref: SharedPreferences
    lateinit var editor: SharedPreferences.Editor

    //private lateinit var progressDialog: ProgressDialog
    private lateinit var binding: FragmentHomeBinding
    private var selectedtype = "Ultrafast"
    private lateinit var progressDialog: AlertDialog

    //this receiver will trigger when the compression is completed
    private val videoCompressionCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Constants.WORK_COMPLETED_ACTION) {
                progressDialog.dismiss()
                // Do something when the WorkManager completes its work
                // For example, update UI, show a notification, etc.

                Log.d("Service", "Broadcast run")

                if (intent.getStringExtra(RETURN_CODE).equals("0")) { //0 means success
                    var msg1 = getString(R.string.notification_message_success)
                    Toast.makeText(context, "$msg1", Toast.LENGTH_SHORT).show()

                    showDataFromPref()
                    var msg2 = getString(R.string.scroll)
                    Toast.makeText(context, "$msg2", Toast.LENGTH_SHORT).show()
                } else {
                    var msg1 = getString(R.string.notification_message_failure)
                    Toast.makeText(context, "$msg1", Toast.LENGTH_SHORT).show()
                }
                if (compressedFilePath.equals(intent.getStringExtra(URI_PATH))) {

                }else
                {
                    compressedFilePath = intent.getStringExtra(URI_PATH).toString()
                }
            }

        }
    }

    private val videoCompressionProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == Constants.WORK_PROGRESS_ACTION) {

                // Do something when the WorkManager completes its work
                // For example, update UI, show a notification, etc.
                if (intent.getStringExtra(RETURN_CODE).equals("0")) { //0 means success
                    val percentage = intent.getStringExtra("percentage")
                    //progressDialog.setMessage("Please wait...$percentage")
                    var msg = getString(R.string.waiting)
                    progressDialog.setMessage("$msg" + "$percentage")
                    if (progressDialog.isShowing.not()) {
                        progressDialog.show()
                    }
                } else {
                    var msg2 = getString(R.string.notification_message_failure)
                    Toast.makeText(context, "$msg2", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    private fun showStats(
        initialSize: String?,
        compressedSize: String?,
        conversionTime: String?,
        initialBattery: String?,
        remainingBattery: String?,
        co2: String?
    ) {
        //showing stats data in the textviews
        binding.statsContainer.visibility = View.VISIBLE
        binding.initialSizeTV.text = initialSize
        binding.compressedSizeTV.text = compressedSize
        binding.conversionTimeTV.text = conversionTime
        binding.initialBatteryTV.text = initialBattery
        binding.remainingBatteryTV.text = remainingBattery
        val pollution= co2!!.toDouble()
        if (pollution > 0) {
            binding.co2TV.setTextColor(Color.parseColor("#FF0000"))
            binding.co2TV.text = co2+ "kgCO2"

        }else{
            binding.co2TV.setTextColor(Color.parseColor("#6F9F3A"))
            binding.co2TV.text = co2+ "kgCO2"
        }
        //displaying the share button
        binding.shareVideo.visibility = View.VISIBLE

    }

    // This is the first method automatically called when the activity starts .
    // Here we initialize all the data which we are going to use and attach the
    // xml layout file with the kotlin code to show the layout and make changes in it when needed
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        binding = FragmentHomeBinding.inflate(inflater, container, false)

        pref = requireActivity().getSharedPreferences(
            requireActivity().packageName, Context.MODE_PRIVATE
        )
        editor = pref.edit()

        requireContext().registerReceiver(
            videoCompressionProgressReceiver, IntentFilter(Constants.WORK_PROGRESS_ACTION)
        )
        requireContext().registerReceiver(
            videoCompressionCompletedReceiver, IntentFilter(Constants.WORK_COMPLETED_ACTION)
        )

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    clearPref()
                    requireActivity().finish()
                }

            })

        return binding.root
    }

    private fun showDataFromPref() {

        if (pref.getString(RETURN_CODE, "")?.isNotEmpty() == true) {
            showStats(
                pref.getString(INITIAL_SIZE, ""),
                pref.getString(COMPRESS_SZE, ""),
                pref.getString(CONVERSION_TIME, ""),
                pref.getString(INITIAL_BATTERY, ""),
                pref.getString(REMAINING_BATTERY, ""),
                pref.getString(CO2, "")
            )

            clearPref()

        }
    }

    private fun clearPref() {
        editor.clear().commit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadAd()
        checkNotificationPermission()
        initUI()
        showLoader()
    }

    private fun checkNotificationPermission() {
        val notificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        if (!areNotificationsEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create a channel for your notifications
                val channel = NotificationChannel(
                    "channel_id", "My Channel", NotificationManager.IMPORTANCE_DEFAULT
                )
                val notificationManager =
                    requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)

                // Ask the user to allow your app to show notifications
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                startActivity(intent)
            } else {
                // Ask the user to allow your app to show notifications
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                startActivity(intent)
            }

        }

    }

    override fun onResume() {
        super.onResume()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }

        Log.d("service", "OnResume")
        showDataFromPref()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().unregisterReceiver(videoCompressionCompletedReceiver)
        requireActivity().unregisterReceiver(videoCompressionProgressReceiver)
    }

    private fun showLoader() {

        val builder = AlertDialog.Builder(
            requireActivity()
        )
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        if (inflater != null) {
            val dialogView = inflater.inflate(R.layout.progress_dialog_layout, null)
            builder.setView(dialogView)
        }
        builder.setCancelable(false)
        progressDialog = builder.create()

    }

    private fun loadAd() {
        MobileAds.initialize(requireActivity()) {}
        val adRequest = AdRequest.Builder().build()
        mAdView = binding.root.findViewById(R.id.adView)
        mAdView.loadAd(adRequest)

    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(requireActivity().packageName)) {
            return true
        } else {
            showAlertDialog()
            return false

        }
    }

    private fun showAlertDialog() {
        AlertDialog.Builder(requireActivity()).apply {
            var msg1 = getString(R.string.notification_background_title)
            var msg2 = getString(R.string.notification_background_body)
            setTitle("$msg1")
            setMessage("$msg2").setPositiveButton(
                "OK"
            ) { _, _ -> openBatteryUsagePage(requireActivity()) }
        }.create().show()
    }

    fun openBatteryUsagePage(ctx: Context) {
        val powerUsageIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        /*val resolveInfo = ctx.packageManager.resolveActivity(powerUsageIntent, 0)
        // check that the Battery app exists on this device
        if (resolveInfo != null) {
            ctx.startActivity(powerUsageIntent)
        } else Toast.makeText(
            ctx,
            "Battery Setting not found in this device please manually go to setting and enable background task",
            Toast.LENGTH_LONG
        ).show()*/

        powerUsageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        powerUsageIntent.data = uri
        try {
            startActivity(powerUsageIntent)
        } catch (e: Exception) {
            Toast.makeText(
                ctx,
                "Battery Setting not found in this device please manually go to setting and enable background task",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Here we are initialising everything and setting click listeners on the code . Like what will happen
    // When the user tap on pick video button and other buttons
    private fun initUI() = with(binding) {
        pickVideo.setOnClickListener {

            if (isBatteryOptimizationDisabled()) {
                shareVideo.visibility = View.GONE
                val intent = Intent(Intent.ACTION_PICK)
                intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")
                resultLauncher.launch(intent)
            }

        }

        // Setting media controller to the video . So the user can pause and play the video . They will appear when user tap on video
        videoView.setMediaController(MediaController(requireActivity()))

        // Handling what will happen when user tap on video compression formats Radio Buttons
        radioGroup.setOnCheckedChangeListener { radioGroup, i ->
            val checked =
                requireActivity().findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
            selectedtype = checked.text.toString()
            when (selectedtype) {
                "Good" ->
                    binding.dataTV.text = "Best quality vs. compression speed and size reduction"
                "Best but slow" ->
                    binding.dataTV.text = "Best size reduction and quality but it's the slowest"
                "Ultrafast" ->
                    binding.dataTV.text = "Fastest and smallest video, loosing some quality"
                "Bueno" ->
                    binding.dataTV.text = "Mejor relación calidad-velocidad de procesamiento y reducción del tamaño"
                "El mejor, pero lento" ->
                    binding.dataTV.text = "Mejor reducción del tamaño y calidad, pero más lento"
                "Ultra-rápido" ->
                    binding.dataTV.text = "El más rápido y menor tamaño posible, perdiendo algo de calidad"
            }
        }

        compressVideo.setOnClickListener {

            clearPref()
            statsContainer.visibility = View.GONE
            shareVideo.visibility = View.GONE

            if (videoUrl != null) {

                val value =
                    fileSize(videoUrl!!.length(requireActivity().contentResolver))
                editor.putString(INITIAL_SIZE, value)
                editor.commit()
                //progressDialog.show()

                // When the compress video button is clicked we check if video is already playing then we pause it
                if (videoView.isPlaying) {
                    videoView.pause()
                }

                // Set up the input data for the worker
                val data2 =
                    Data.Builder().putString(ForegroundWorker.VideoURI, videoUrl?.toString())
                        .putString(ForegroundWorker.SELECTION_TYPE, selectedtype).build()
                // Create the work request
                val myWorkRequest =
                    OneTimeWorkRequestBuilder<VideoCompressionWorker>().setInputData(data2).build()

                //initiating WorkManager to start compressing the video
                WorkManager.getInstance(requireContext()).enqueue(myWorkRequest)

            } else {
                // If picked video is null or video is not picked
                Toast.makeText(context, "Please select a video.", Toast.LENGTH_SHORT).show()
            }

        }


        binding.shareVideo.setOnClickListener {
            ShareCompat.IntentBuilder(requireActivity()).setStream(Uri.parse(compressedFilePath))
                .setType("video/mp4").setChooserTitle("Share video...").startChooser()
        }
    }


    /* This code is using the registerForActivityResult method to launch an activity for a result,
    specifically to select a video file. If the result code is Activity.RESULT_OK, it means a video has been successfully selected.
     The selected video's Uri is extracted from the Intent returned from the launched activity.
      The code then sets the Uri to the VideoView and starts playing the video.
      If there is an error in the process, an error message is displayed to the user via a Toast. */
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data

                if (data != null) {
                    // get the video Uri
                    val uri: Uri? = data.data
                    try {
                        // get the file from the Uri using getFileFromUri() method present
                        // in FileUils.java
                        videoUrl = uri


                        //   val video_file: File? = uri?.let { FileUtils().getFileFromUri(this, it) }

                        // now set the video uri in the VideoView
                        binding.videoView.setVideoURI(uri)

                        // after successful retrieval of the video and properly
                        // setting up the retried video uri in
                        // VideoView, Start the VideoView to play that video
                        binding.videoView.start()

                    } catch (e: Exception) {
                        Toast.makeText(requireActivity(), "Error", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }

            }
        }


    companion object {
        fun newInstance() = HomeFragment()

        const val URI_PATH = "uri_path"
        const val RETURN_CODE = "return_code"
        const val INITIAL_SIZE = "initial_size"
        const val COMPRESS_SZE = "compressed_size"
        const val CONVERSION_TIME = "conversion_time"
        const val INITIAL_BATTERY = "initial_battery"
        const val REMAINING_BATTERY = "remaining_battery"
        const val CO2 = "co2"
    }


}