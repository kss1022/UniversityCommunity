package net.suwon.plus.ui.main.time_table

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import net.suwon.plus.R
import net.suwon.plus.data.entity.timetable.DayOfTheWeek
import net.suwon.plus.data.entity.timetable.TimeTableCellEntity
import net.suwon.plus.data.entity.timetable.TimeTableLocationAndTime
import net.suwon.plus.data.entity.timetable.TimeTableWithCell
import net.suwon.plus.databinding.FragmentTimeTableBinding
import net.suwon.plus.extensions.fromDpToPx
import net.suwon.plus.model.TimeTableCellModel
import net.suwon.plus.ui.base.BaseFragment
import net.suwon.plus.ui.main.time_table.addTimeTable.addcell.AddTimeTableCellActivity
import net.suwon.plus.ui.main.time_table.addTimeTable.tablelist.TimeTableListActivity
import javax.inject.Inject


class TimeTableFragment : BaseFragment<TimeTableViewModel, FragmentTimeTableBinding>() {

    @Inject
    override lateinit var viewModelFactory: ViewModelProvider.Factory

    override val viewModel: TimeTableViewModel by viewModels<TimeTableViewModel> { viewModelFactory }

    override fun getViewBinding(): FragmentTimeTableBinding =
        FragmentTimeTableBinding.inflate(layoutInflater)

    private val addTimeTableCellLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {}
                removeAddedView()
                viewModel.fetchData()
        }

    private val timeTableListLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                removeAddedView()
                viewModel.fetchData()
            }
        }


    private lateinit var timeTableWithCell: TimeTableWithCell

    private var clicked = false
    private val rotateAnimationOpen : Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.button_rotate_open) }
    private val rotateAnimationClose : Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.button_rotate_close) }
    private val slideAnimationUp :Animation  by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.button_slide_left) }
    private val slideAnimationDown :Animation  by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.button_slide_right) }



    override fun initViews() {
        bindViews()
    }


    private fun bindViews() = with(binding) {
        addButton.setOnClickListener {

            if(!clicked){
                directAddButton.visibility = View.VISIBLE
                searchAddButton.visibility = View.VISIBLE
                directAddButton.startAnimation(slideAnimationUp)
                searchAddButton.startAnimation(slideAnimationUp)
                addButton.startAnimation(rotateAnimationOpen)
            }else{
                directAddButton.isGone = true
                searchAddButton.isGone = true
                directAddButton.startAnimation(slideAnimationDown)
                searchAddButton.startAnimation(slideAnimationDown)
                addButton.startAnimation(rotateAnimationClose)
            }


            clicked = !clicked
        }

        searchAddButton.setOnClickListener {
            if(::timeTableWithCell.isInitialized){
                addTimeTableCellLauncher.launch(
                    AddTimeTableCellActivity.newIntent(requireContext(), timeTableWithCell)
                )
            }
        }


        directAddButton.setOnClickListener {
            TimeTableBottomSheetFragment.newInstance(
                timeTableWithCell,
                null
            ) { timeTableCellEntity, i ->

                when (i) {
                    TimeTableBottomSheetFragment.NEW -> {
                        timeTableCellEntity?.let {
                            removeAddedView()
                            viewModel.addTimeTableEntity(timeTableCellEntity)
                        }
                    }
                    else -> Unit
                }
            }.show(requireActivity().supportFragmentManager, TimeTableBottomSheetFragment.TAG)
        }


        listButton.setOnClickListener {
            if (::timeTableWithCell.isInitialized) {
                timeTableListLauncher.launch(
                    TimeTableListActivity.newInstance(requireContext(), timeTableWithCell)
                )
                activity?.overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
            }
        }

        addTimeTableButton.setOnClickListener {
            createTimeTableAlertDialog()
        }
    }


    override fun observeData() = viewModel.timeTableStateLiveData.observe(viewLifecycleOwner) {
        when (it) {
            is TimeTableState.Loading -> {
                handleLoadingState()
            }

            is TimeTableState.NoTable -> {
                handleNoTableState()
            }

            is TimeTableState.Success -> {
                handleSuccessState(it)
            }

            is TimeTableState.Error -> {
                handleErrorState(it)
            }
            else -> Unit
        }
    }


    private fun handleLoadingState() = with(binding) {
        progressBar.visibility = View.VISIBLE
        errorMessageTextView.isGone = true

        scrollView.isGone = true
        addButton.isEnabled = false
        listButton.isEnabled = false
    }


    private fun handleSuccessState(timeTableState: TimeTableState.Success) = with(binding) {
        progressBar.isGone = true
        errorMessageTextView.isGone = true

        scrollView.visibility = View.VISIBLE
        noTimeTableTextView.isGone = true
        addTimeTableButton.isGone = true

        addButton.isEnabled = true
        listButton.isEnabled = true

        timeTableWithCell = timeTableState.timeTableWithCell

        addLectureList(
            timeTableState.timeTableWithCell.timeTableCellList.map {
                it.toModel()
            }
        )


        toolBarTimeTableNameTextView.text =
            if (timeTableState.timeTableWithCell.timeTable.tableName.isEmpty()) getString(R.string.time_table) else timeTableState.timeTableWithCell.timeTable.tableName
        toolBarTimeTableSeasonTextview.text = getString(
            R.string.timetable_season,
            timeTableState.timeTableWithCell.timeTable.year,
            timeTableState.timeTableWithCell.timeTable.semester
        )
    }

    private fun handleNoTableState() = with(binding) {
        progressBar.isGone = true
        errorMessageTextView.isGone = true

        scrollView.visibility = View.VISIBLE
        noTimeTableTextView.visibility = View.VISIBLE
        noTimeTableTextView.text =
            getString(R.string.is_not_exist_timetable_please_create_timetable)

        addTimeTableButton.visibility = View.VISIBLE
        addTimeTableButton.text = getString(R.string.create_new_timetable)

        addButton.isEnabled = false
        listButton.isEnabled = false

        toolBarTimeTableNameTextView.text = ""
    }


    private fun handleErrorState(timeTableState: TimeTableState.Error) = with(binding) {
        progressBar.isGone = true
        errorMessageTextView.visibility = View.VISIBLE

        scrollView.isGone = true

        addButton.isEnabled = false
        listButton.isEnabled = false

        errorMessageTextView.text = getString(timeTableState.massageId)
    }


    private fun createTimeTableAlertDialog() {
        val numberPickers =
            LayoutInflater.from(requireContext()).inflate(R.layout.timetable_date_picker, null)

        val year = getYearNumberPicker(numberPickers)
        val semester = getSemesterNumberPicker(numberPickers)
        val tableName = numberPickers.findViewById<EditText>(R.id.tableNameEditText)



        AlertDialog.Builder(requireContext())
            .setView(numberPickers)
            .setTitle("시간표 만들기")
            .setPositiveButton("확인") { dialog, _ ->


                //year.displayedValues[year.value - MIN_YEAR]
                //semester.value


                viewModel.saveNewTimeTable(
                    tableName.text.toString(),
                    (year.displayedValues[year.value - MIN_YEAR]).substring(0..3).toInt(),
                    semester.value
                )

                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }


    private fun getYearNumberPicker(numberPickers: View): NumberPicker =
        numberPickers.findViewById<NumberPicker>(R.id.yearNumberPicker).apply {
            wrapSelectorWheel = false
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS

            maxValue = DEFAULT_YEAR
            minValue = MIN_YEAR

            val yearDisplayArray = arrayListOf<String>()

            for (i in 0..(DEFAULT_YEAR - MIN_YEAR)) {
                yearDisplayArray.add("${DEFAULT_YEAR - i}년도")
            }

            displayedValues = yearDisplayArray.toArray(arrayOfNulls<String>(yearDisplayArray.size))
        }


    private fun getSemesterNumberPicker(numberPickers: View): NumberPicker =
        numberPickers.findViewById<NumberPicker>(R.id.semesterNumberPicker).apply {
            maxValue = 2
            minValue = 1

            displayedValues = arrayOf(
                "1학기", "2학기"
            )
        }


    private var addButtonList: ArrayList<Triple<Long, Int, Int>> = arrayListOf()
    private var addNoTimeTextList: ArrayList<Pair<Long, Int>> = arrayListOf()

    private var addedGridViewList: ArrayList<Int> = arrayListOf()
    private var addedTextViewList: ArrayList<Int> = arrayListOf()


    var minTime = DEFAULT_MIN_TIME


    private fun addLectureList(timetableCellModelList: List<TimeTableCellModel>) {
        checkTimeAndAddGridView(timetableCellModelList)

        timetableCellModelList.forEach { cell ->
            addLectureOnView(requireContext(), cell)
        }
    }


    private fun addLectureOnView(context: Context, model: TimeTableCellModel) {

        if (model.locationAndTimeList.isEmpty()) {
            //시간이 없는 경우
            addNoTimeLecture(model, null)

        } else {
            model.locationAndTimeList.forEach { locationAndTime ->
                val location = locationAndTime.location
                val day = locationAndTime.day
                val time = locationAndTime.time

                if (day == DayOfTheWeek.DEFAULT) {
                    //토요일 인 경우
                    addNoTimeLecture(model, locationAndTime)
                }


                val button = createButton(context, model, location, time)
                addButton(day, button, model)
            }
        }

    }


    private fun checkTimeAndAddGridView(timetableCellModelList: List<TimeTableCellModel>) {
        var maxTime = DEFAULT_MAX_TIME
        minTime = DEFAULT_MIN_TIME

        timetableCellModelList.forEach { timetableCell ->
            timetableCell.locationAndTimeList.forEach {
                if (it.day != DayOfTheWeek.DEFAULT) {
                    val tableMaxTime = it.time.second / 60
                    if (tableMaxTime > maxTime) maxTime = tableMaxTime

                    val tableMinTime = it.time.first / 60
                    if (tableMinTime < minTime) minTime = tableMinTime
                }
            }
        }

        if (maxTime > DEFAULT_MAX_TIME) {
            for (i in (DEFAULT_MAX_TIME + 1)..maxTime) {
                addGrid(i)
            }
        }

        if (minTime < DEFAULT_MIN_TIME) {
            for (i in (DEFAULT_MIN_TIME - 1) downTo minTime) {
                addGrid(i)
            }
        }

    }

    private fun addGrid(i: Int) {
        val gridView = View(requireContext()).apply {

            setBackgroundResource(R.color.colorPrimary)

            id = ViewCompat.generateViewId()

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            lp.height = 1.fromDpToPx()


            layoutParams = lp
        }

        val textView = TextView(requireContext()).apply {
            width = 20.fromDpToPx()
            height = 60.fromDpToPx()

            text = if (i > DEFAULT_MAX_TIME) "${i - 12}" else "$i"

            id = ViewCompat.generateViewId()

            gravity = Gravity.END or Gravity.TOP
            setPadding(0.fromDpToPx(), 0.fromDpToPx(), 3.fromDpToPx(), 0.fromDpToPx())


            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            lp.gravity = Gravity.START
            layoutParams = lp
        }



        if (i > DEFAULT_MAX_TIME) {
            binding.leftLinearLayout.addView(gridView)
            binding.leftLinearLayout.addView(textView)
        } else {
            binding.leftLinearLayout.addView(textView, 0)
            binding.leftLinearLayout.addView(gridView, 0)
        }


        addedGridViewList.add(gridView.id)
        addedTextViewList.add(textView.id)
    }


    private fun createButton(
        context: Context,
        model: TimeTableCellModel,
        location: String,
        time: Pair<Int, Int>
    ): Button = Button(context).apply {
        text = getString(R.string.timetable_cell_title, model.name, location)
        setTextColor(ContextCompat.getColor(context, model.textColor))
        typeface = Typeface.DEFAULT_BOLD
        textSize = 10f
        gravity = Gravity.START


        setBackgroundColor(ContextCompat.getColor(context, model.cellColor))
        stateListAnimator = null

        setPadding(6.fromDpToPx(), 6.fromDpToPx(), 6.fromDpToPx(), 6.fromDpToPx())

        id = ViewCompat.generateViewId()

        setOnClickListener { view ->
            val modelId = addButtonList.filter {
                it.second == view.id
            }.first().first

            if (::timeTableWithCell.isInitialized) {
                val clickItem = timeTableWithCell.timeTableCellList.find {
                    it.cellId == modelId
                } ?: return@setOnClickListener

                showTimeTableInfoAlertDialog(clickItem)
            }
        }


        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )


        var minutes = time.second - time.first
        minutes += (minutes / 60)

        var marginTop = (time.first - minTime * 60)
        marginTop += marginTop / 60

        lp.height = minutes.fromDpToPx()
        lp.gravity = Gravity.START
        lp.topMargin = marginTop.fromDpToPx()


        layoutParams = lp
    }


    private fun addButton(
        day: DayOfTheWeek,
        button: Button,
        model: TimeTableCellModel
    ) {
        when (day) {
            DayOfTheWeek.MON -> {
                binding.monLinearLayout.addView(button)
                addButtonList.add(
                    Triple(
                        model.id,
                        button.id,
                        binding.monLinearLayout.id
                    )
                )
            }

            DayOfTheWeek.TUE -> {
                binding.tueLinearLayout.addView(button)
                addButtonList.add(
                    Triple(
                        model.id,
                        button.id,
                        binding.tueLinearLayout.id
                    )
                )
            }

            DayOfTheWeek.WED -> {
                binding.wedLinearLayout.addView(button)
                addButtonList.add(
                    Triple(
                        model.id,
                        button.id,
                        binding.wedLinearLayout.id
                    )
                )
            }

            DayOfTheWeek.THU -> {
                binding.thuLinearLayout.addView(button)
                addButtonList.add(
                    Triple(
                        model.id,
                        button.id,
                        binding.thuLinearLayout.id
                    )
                )
            }

            DayOfTheWeek.FRI -> {
                binding.friLinearLayout.addView(button)
                addButtonList.add(
                    Triple(
                        model.id,
                        button.id,
                        binding.friLinearLayout.id
                    )
                )
            }

            else -> Unit
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addNoTimeLecture(
        model: TimeTableCellModel,
        locationAndTime: TimeTableLocationAndTime?
    ) {
        val tv = TextView(requireContext()).apply {
            locationAndTime?.let {
                text =
                    "${model.name}  [ ${locationAndTime.day.char}${locationAndTime.getTimeString()} ]"
            } ?: kotlin.run {
                text = model.name
            }

            id = ViewCompat.generateViewId()

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(12.fromDpToPx(), 12.fromDpToPx(), 12.fromDpToPx(), 12.fromDpToPx())

            layoutParams = lp
            background = null
            setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryVariant))


            setOnClickListener { view ->
                val modelId = addNoTimeTextList.filter {
                    it.second == view.id
                }.first().first

                if (::timeTableWithCell.isInitialized) {
                    val clickItem = timeTableWithCell.timeTableCellList.find {
                        it.cellId == modelId
                    } ?: return@setOnClickListener

                    showTimeTableInfoAlertDialog(clickItem)
                }
            }
        }

        binding.noTimeLectureList.addView(tv)
        addNoTimeTextList.add(model.id to tv.id)
    }


    private fun removeAddedView() {
        addButtonList.forEach {
            val view = binding.root.findViewById<FrameLayout>(it.third)
            view.removeView(view.findViewById(it.second))
        }

        binding.noTimeLectureList.let { linearLayout ->
            addNoTimeTextList.forEach {
                linearLayout.removeView(linearLayout.findViewById(it.second))
            }
        }



        binding.leftLinearLayout.let { leftLinearLayout ->
            addedGridViewList.forEach {
                leftLinearLayout.removeView(leftLinearLayout.findViewById(it))
            }


            addedTextViewList.forEach {
                leftLinearLayout.removeView(leftLinearLayout.findViewById(it))
            }
        }

        addButtonList.clear()
        addNoTimeTextList.clear()
        addedGridViewList.clear()
        addedTextViewList.clear()
    }


    private fun showTimeTableInfoAlertDialog(clickItem: TimeTableCellEntity) {
        val alertDialog = AlertDialog.Builder(context).create()

        val alertDialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.time_table_info_alert_dialog, null).apply {
                var timeStr = ""
                var locationStr = ""
                val location = mutableSetOf<String>()

                clickItem.locationAndTimeList.forEach {
                    timeStr += "${it.getTimeString()}  "
                    location.add(it.location)
                }

                location.forEach {
                    locationStr += "$it  "
                }


                this.findViewById<View>(R.id.titleColorView)
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            clickItem.cellColor
                        )
                    )
                this.findViewById<TextView>(R.id.titleTextView).text = clickItem.name
                this.findViewById<TextView>(R.id.professorNameTextView).text =
                    clickItem.professorName
                this.findViewById<TextView>(R.id.timeTextView).text = timeStr
                this.findViewById<TextView>(R.id.locationTextView).text = locationStr

                this.findViewById<ImageView>(R.id.editButton).setOnClickListener {
                    TimeTableBottomSheetFragment.newInstance(
                        timeTableWithCell,
                        clickItem
                    ) { timeTableCellEntity, i ->
                        when (i) {
                            TimeTableBottomSheetFragment.EDIT -> {
                                timeTableCellEntity?.let {
                                    removeAddedView()
                                    viewModel.updateTimeTableEntity(timeTableCellEntity)
                                }
                            }

                            TimeTableBottomSheetFragment.DELETE -> {

                                timeTableCellEntity?.let {
                                    removeAddedView()
                                    viewModel.deleteTimeTableEntity(timeTableCellEntity)
                                }
//
//                            addButtonList.filter {
//                                it.first == modelId
//                            }.forEach {
//                                val view = binding.root.findViewById<FrameLayout>(it.third)
//                                view.removeView(view.findViewById(it.second))
//                            }
                            }

                            else -> Unit
                        }
                    }.show(
                        requireActivity().supportFragmentManager,
                        TimeTableBottomSheetFragment.TAG
                    )

                    alertDialog.dismiss()
                }
            }


        alertDialog.setView(alertDialogView)
        alertDialog.show()
    }


    companion object {
        fun newInstance() = TimeTableFragment()

        const val TAG = "TimeTableFragment"

        const val DEFAULT_YEAR = 2022
        const val MIN_YEAR = 2015

        private const val DEFAULT_MAX_TIME = 16
        private const val DEFAULT_MIN_TIME = 9
    }
}