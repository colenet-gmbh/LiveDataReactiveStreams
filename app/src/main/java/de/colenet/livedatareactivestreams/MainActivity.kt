package de.colenet.livedatareactivestreams

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import de.colenet.livedatareactivestreams.databinding.ActivityMainBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    val viewModel: MainViewModel by viewModel()
    val TAG = ">>>>>> THREADING"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding  = DataBindingUtil.setContentView(
            this, R.layout.activity_main)
        binding.setLifecycleOwner(this)
        binding.viewModel = viewModel

        Log.e(TAG, "Triggering refresh on thread ${Thread.currentThread().id}")
        setContentView(binding.root)

        viewModel.triggerPing()
    }
}
