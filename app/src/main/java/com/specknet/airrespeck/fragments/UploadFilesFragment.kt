package com.specknet.airrespeck.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.specknet.airrespeck.R
import com.specknet.airrespeck.utils.Constants
import com.specknet.airrespeck.utils.Utils
import java.io.File
import java.util.*
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import kotlin.collections.HashMap

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [UploadFilesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UploadFilesFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    lateinit var storage: FirebaseStorage
    var totalBytesForUploading = 0L
    var totalBytesTransferred = 0L
    lateinit var progressBar: ProgressBar
    lateinit var progressBarLabel: TextView
    var totalFilesToUpload = 0
    var totalFilesAlreadyUploaded = 0
    lateinit var spinningCircle: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_upload_files, container, false)

        val uploadButton: Button = view.findViewById(R.id.upload_files_button)
        storage = FirebaseStorage.getInstance()

        progressBar = view.findViewById(R.id.upload_progress_bar)
        progressBarLabel = view.findViewById(R.id.progress_bar_label)
        spinningCircle = view.findViewById(R.id.spinning_circle)

        progressBar.visibility = View.INVISIBLE
        progressBarLabel.visibility = View.INVISIBLE
        spinningCircle.visibility = View.INVISIBLE

        uploadButton.setOnClickListener {
            uploadFiles()
        }

        return view
    }

    fun updateProgressBar(bytesTransferred: Long) {
        if (progressBar.visibility == View.INVISIBLE) {
            stopSpinningCircle()
            progressBar.visibility = View.VISIBLE
            progressBarLabel.visibility = View.VISIBLE
            progressBarLabel.text = "Upload progress"
        }

        totalBytesTransferred  += bytesTransferred

        val progress = (100.0 * totalBytesTransferred) / totalBytesForUploading
        progressBar.progress = progress.toInt()

        if (progress >= 100) {
            displaySuccessMessage()
        }
    }

    fun displaySuccessMessage() {
        progressBar.visibility = View.INVISIBLE
        progressBarLabel.text = "Upload complete!"
    }

    fun displayAlreadyUploaded() {
        progressBar.visibility = View.INVISIBLE
        stopSpinningCircle()

        progressBarLabel.text = "Data already uploaded!"
        progressBarLabel.visibility = View.VISIBLE
    }

    fun startSpinnningCircle() {
        progressBar.visibility = View.INVISIBLE
        progressBarLabel.visibility = View.INVISIBLE
        spinningCircle.visibility = View.VISIBLE
    }

    fun stopSpinningCircle() {
        spinningCircle.visibility = View.INVISIBLE
    }

    fun uploadFiles() {
        Toast.makeText(activity, "Uploading all the files", Toast.LENGTH_LONG).show()

        startSpinnningCircle()

        val storageRef: StorageReference = storage.getReferenceFromUrl("gs://airrespeck.appspot.com/")

        // Upload content
        val folderName = Utils.getInstance().getDataDirectory(activity)
        Log.i("Firebase", "Folder name I want to upload is $folderName")

        val folderFile = File(folderName)
        val filesInFolder = folderFile.listFiles()

        if (filesInFolder != null) {
            Log.i("Firebase", "Folder size = ${filesInFolder.size}")

            for(i in filesInFolder.indices) {

                Log.i("Firebase", "File: ${filesInFolder[i]}")

                // for each of these, look inside and see if there are any files to upload
                val subFiles = filesInFolder[i].listFiles()

                if (subFiles != null) {
                    for (j in subFiles.indices) {
                        totalFilesToUpload += 1
                        val currentFile = subFiles[j].absolutePath
                        Log.i("Firebase", "Subfile ${currentFile}")

                        val fileUri = Uri.fromFile(subFiles[j])
                        val fileExtension = currentFile.substring(currentFile.lastIndexOf(".") + 1)

                        val metadata = StorageMetadata.Builder()
                                .setContentType("AirRespeck/${fileExtension}")
                                .build();

                        // Upload file and metadata to the path
                        val indexOfAirRespeck = fileUri.path?.indexOf("AirRespeck")
                        val pathFromAirRespeck = fileUri.path?.substring(indexOfAirRespeck!! + "AirRespeck/".length)

                        val uploadRef = storageRef.child("AirRespeck/${pathFromAirRespeck}")
                                .metadata
                                .addOnSuccessListener {
                                    Log.i("Firebase", "File exists! Skipping")
                                    totalFilesAlreadyUploaded += 1

                                    if (totalFilesAlreadyUploaded == totalFilesToUpload) {
                                        displayAlreadyUploaded()
                                    }
                                }
                                .addOnFailureListener {
                                    Log.i("Firebase", "File does not exist, uploading.")

                                    val uploadTask = storageRef.child("AirRespeck/${pathFromAirRespeck}").putFile(fileUri, metadata)

                                    totalBytesForUploading += subFiles[j].length()

                                    // for each file remember the last upload stat
                                    var lastBytesUploaded = 0L

                                    // Listen for state changes, errors, and completion of the upload.
                                    // You'll need to import com.google.firebase.storage.ktx.component1 and
                                    // com.google.firebase.storage.ktx.component2
                                    uploadTask.addOnProgressListener { (bytesTransferred, totalByteCount) ->

                                        if (lastBytesUploaded == 0L) {
                                            // first time update the progress bar with how many bytes were transferred
                                            updateProgressBar(bytesTransferred)
                                        }
                                        else {
                                            val bytesTransferredSinceLastTime = bytesTransferred - lastBytesUploaded//filesAndBytesUploaded[pathFromAirRespeck]!!
                                            updateProgressBar(bytesTransferredSinceLastTime)
                                        }
                                        lastBytesUploaded = bytesTransferred

                                        val progress = (100.0 * bytesTransferred) / totalByteCount
                                        Log.d("Firebase", "Upload for file ${subFiles[j]} is $progress% done")

                                    }.addOnPausedListener {
                                        Log.d("Firebase", "Upload is paused")
                                    }.addOnFailureListener {
                                        // Handle unsuccessful uploads
                                        Log.d("Firebase", "Upload unsuccessful!")
                                        it.printStackTrace()
                                    }.addOnSuccessListener {
                                        // Handle successful uploads on complete
                                        // ...
                                        Log.d("Firebase", "Upload successful!")
                                    }
                                }
                    }
                }

            }
        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UploadFilesFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                UploadFilesFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}