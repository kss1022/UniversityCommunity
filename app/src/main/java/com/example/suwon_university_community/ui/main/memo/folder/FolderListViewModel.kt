package com.example.suwon_university_community.ui.main.memo.folder

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.suwon_university_community.data.entity.memo.FolderCategory
import com.example.suwon_university_community.data.entity.memo.FolderEntity
import com.example.suwon_university_community.data.repository.memo.MemoRepository
import com.example.suwon_university_community.data.repository.timetable.TimeTableRepository
import com.example.suwon_university_community.model.FolderModel
import com.example.suwon_university_community.ui.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject



class FolderListViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val timeTableRepository: TimeTableRepository
) : BaseViewModel() {

    val memoStateLiveData = MutableLiveData<FolderListState>(FolderListState.Uninitialized)
    val timeTableCountLiveData = MutableLiveData<Pair<FolderModel, Int>>()
    val folders: MutableStateFlow<List<FolderEntity>> = MutableStateFlow(emptyList())

    override fun fetchData(): Job = viewModelScope.launch {
        memoStateLiveData.value = FolderListState.Loading
        memoRepository.getFolderList().onEach { folders.value = it }.launchIn(this)
        memoStateLiveData.value = FolderListState.Success
    }

    fun addFolder(folderName: String, folderCategory: FolderCategory) = viewModelScope.launch {
        memoRepository.insertFolder(
            FolderEntity(
                name = folderName,
                category = folderCategory
            )
        )
    }

    fun checkTimeTableCount(model: FolderModel) = viewModelScope.launch {
        timeTableRepository.getTimeTableCount()?.let {
            timeTableCountLiveData.value = model to it
        }
    }

    fun deleteFolder(model: FolderModel) = viewModelScope.launch {
        memoRepository.deleteFolder(model.id)
    }

    fun changeFolderName(model: FolderModel, name: String) = viewModelScope.launch{
        val updateFolder =
            FolderEntity(
                folderId = model.id,
                name = name,
                count = model.count,
                category = model.category,
                isDefault = model.isDefault,
                timeTableId = model.timeTableId
            )

        memoRepository.updateFolder(updateFolder)
    }
}