package net.suwon.plus.ui.main.time_table.addTimeTable.addcell.lecturelist

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import net.suwon.plus.data.entity.lecture.CollegeCategory
import net.suwon.plus.databinding.FragmentLectureListBinding
import net.suwon.plus.model.LectureModel
import net.suwon.plus.ui.base.BaseFragment
import net.suwon.plus.ui.main.time_table.addTimeTable.addcell.AddTimeTableCellActivity
import net.suwon.plus.ui.main.time_table.addTimeTable.addcell.AddTimeTableCellSharedViewModel
import net.suwon.plus.util.provider.DefaultResourceProvider
import net.suwon.plus.widget.adapter.ModelRecyclerViewAdapter
import net.suwon.plus.widget.adapter.listener.LectureListAdapterListener
import javax.inject.Inject

class LectureListFragment : BaseFragment<LectureListViewModel, FragmentLectureListBinding>() {

    @Inject
    override lateinit var viewModelFactory: ViewModelProvider.Factory

    override val viewModel: LectureListViewModel by viewModels<LectureListViewModel> { viewModelFactory }
    private val sharedViewModel: AddTimeTableCellSharedViewModel by activityViewModels()

    override fun getViewBinding(): FragmentLectureListBinding =
        FragmentLectureListBinding.inflate(layoutInflater)

    @Inject
    lateinit var resourceProvider: DefaultResourceProvider


    private val modelAdapter by lazy {
        ModelRecyclerViewAdapter<LectureModel, LectureListViewModel>(
            modelList = listOf(),
            viewModel,
            resourcesProvider = resourceProvider,
            adapterListener = object : LectureListAdapterListener {
                override fun selectLecture(model: LectureModel) {
                    viewModel.checkTimeTableAndAdd(
                        (activity as AddTimeTableCellActivity).timetableWithCell.timeTable.tableId,
                        model
                    )
                }
            }
        )
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.category = arguments?.getSerializable(CATEGORY) as CollegeCategory
        super.onViewCreated(view, savedInstanceState)
    }


    override fun initViews() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = modelAdapter
        }
    }


    override fun observeData() {
        viewModel.lectureListLiveData.observe(viewLifecycleOwner) { lectureList ->

           lifecycleScope.launch {
               lectureList?.let { list->
                   if(list.isNotEmpty()){
                       modelAdapter.submitList(lectureList.map { it.toLectureModel() })
                   }
               }

//               if(lectureList.isNotEmpty()){
//                   modelAdapter.submitList(lectureList.map { it.toLectureModel() })
//               }
           }

        }

        viewModel.lectureListStateLiveData.observe(viewLifecycleOwner) {
            when (it) {

                is LectureListState.Loading -> {
                    handleLoadingState()
                }


                is LectureListState.Success -> {
                    handleSuccessState()
                }

                is LectureListState.Added -> {
                    Toast.makeText(requireContext(), "겹치는 시간이 있습니다😱\n${it.addedMessage}", Toast.LENGTH_SHORT).show()
                }

                is LectureListState.NotAdded -> {
                    sharedViewModel.lectureEntityLiveData.value = it.model
                }

                is LectureListState.Error -> {
                    handleErrorState(it)
                }

                else -> Unit
            }

        }
    }


    private fun handleLoadingState() = with(binding) {
        progressBar.visibility = View.VISIBLE
        errorMessageTextView.isGone = true
        recyclerView.isGone = true
    }


    private fun handleSuccessState() = with(binding) {
        progressBar.isGone = true
        errorMessageTextView.isGone = true
        recyclerView.visibility = View.VISIBLE
    }


    private fun handleErrorState(lectureListState: LectureListState.Error) = with(binding) {
        progressBar.isGone = true
        errorMessageTextView.visibility = View.VISIBLE
        recyclerView.isGone = true
        errorMessageTextView.text = getString(lectureListState.massageId)
    }



    companion object {
        fun newInstance(category: CollegeCategory) = LectureListFragment().apply {
            arguments = bundleOf(
                CATEGORY to category
            )
        }

        private const val CATEGORY = "category"
    }
}