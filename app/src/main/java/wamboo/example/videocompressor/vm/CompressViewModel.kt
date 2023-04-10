package wamboo.example.videocompressor.vm

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import wamboo.example.videocompressor.models.CompressData
import wamboo.example.videocompressor.repository.CompressRepository
import javax.inject.Inject

@HiltViewModel
class CompressViewModel @Inject constructor(private val compressRepository: CompressRepository) :
    ViewModel() {


    fun delete() {
        viewModelScope.launch {
            compressRepository.delete()
        }
    }

    var data: MutableLiveData<Pair<Long, Long>> = MutableLiveData()

    fun setMap(startDate: Long, endDate: Long) {
        data.value = Pair(startDate, endDate)
    }

    val userLiveDataByDate: LiveData<List<CompressData>> = data.switchMap { param1 ->
        compressRepository.getAllUserData(param1.first, param1.second)
    }

    fun deleteSpecific(start: Long, end: Long) {
        viewModelScope.launch {
            compressRepository.deleteSpecific(start, end)
        }
    }

    fun getCount() = compressRepository.getAllCompression()

}