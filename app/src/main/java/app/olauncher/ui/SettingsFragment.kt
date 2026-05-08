package app.olauncher.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olauncher.BuildConfig
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentSettingsBinding
import app.olauncher.helper.animateAlpha
import app.olauncher.helper.appUsagePermissionGranted
import app.olauncher.helper.applyTypefaceRecursively
import app.olauncher.helper.getColorFromAttr
import app.olauncher.helper.isAccessServiceEnabled
import app.olauncher.helper.isTablet
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.openUrl
import app.olauncher.helper.rateApp
import app.olauncher.helper.resolveCustomTypeface
import app.olauncher.helper.shareApp
import app.olauncher.helper.showToast
import app.olauncher.listener.DeviceAdmin

class SettingsFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private val weatherLocationPermissionCode = 8801

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val showPentastic = System.currentTimeMillis() % 2 == 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        viewModel.isOlauncherDefault()

        deviceManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
        checkAdminPermission()

        binding.homeAppsNum.text = prefs.homeAppsNum.toString()
        populateKeyboardText()
        populateScreenTimeOnOff()
        populateLockSettings()
        populateHomeButtonRecents()
        populateAppThemeText()
        populateTextSize()
        populateFont()
        populateAlignment()
        populateWeatherSettings()
        populateStatusBar()
        populateRainbowMode()
        populateDateTime()
        populateSwipeApps()
        populateSwipeDownAction()
        populateActionHints()
        initClickListeners()
        initObservers()

        if (showPentastic)
            binding.footer.text = getText(R.string.new_app_minimal_todo_lists)
        applyCustomFont(view)
    }

    private fun applyCustomFont(view: View) {
        resolveCustomTypeface(requireContext(), prefs.fontFamily)?.let {
            view.applyTypefaceRecursively(it)
        }
    }

    override fun onClick(view: View) {
        binding.dateTimeSelectLayout.visibility = View.GONE
        binding.appThemeSelectLayout.visibility = View.GONE
        binding.swipeDownSelectLayout.visibility = View.GONE
        binding.fontSelectLayout.visibility = View.GONE
        if (view.id != R.id.textSizeMinus && view.id != R.id.textSizePlus) {
            if (binding.textSizesLayout.isVisible) {
                binding.textSizesLayout.visibility = View.GONE
                applyTextSizeScale()
            }
        }
        if (view.id != R.id.alignment)
            binding.alignmentSelectLayout.visibility = View.GONE
        if (view.id != R.id.appListAlignment)
            binding.appListAlignmentSelectLayout.visibility = View.GONE
        if (view.id != R.id.clockAlignment)
            binding.clockAlignmentSelectLayout.visibility = View.GONE
        if (view.id != R.id.weatherSide)
            binding.weatherSideSelectLayout.visibility = View.GONE
        if (view.id != R.id.weatherUnits)
            binding.weatherUnitsSelectLayout.visibility = View.GONE

        when (view.id) {
            R.id.olauncherHiddenApps -> showHiddenApps()
            R.id.screenTimeOnOff -> viewModel.showDialog.postValue(Constants.Dialog.DIGITAL_WELLBEING)
            R.id.appInfo -> openAppInfo(requireContext(), Process.myUserHandle(), BuildConfig.APPLICATION_ID)
            R.id.setLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.toggleLock -> toggleLockMode()
            R.id.homeButtonRecents -> toggleHomeButtonRecents()
            R.id.autoShowKeyboard -> toggleKeyboardText()
            R.id.appsNumMinus -> changeHomeAppsNum(-1)
            R.id.appsNumPlus -> changeHomeAppsNum(1)
            R.id.alignment -> binding.alignmentSelectLayout.visibility = View.VISIBLE
            R.id.alignmentLeft -> viewModel.updateHomeAlignment(Gravity.START)
            R.id.alignmentCenter -> viewModel.updateHomeAlignment(Gravity.CENTER)
            R.id.alignmentRight -> viewModel.updateHomeAlignment(Gravity.END)
            R.id.homeBottomAlignment -> updateHomeBottomAlignment()
            R.id.appListAlignment -> binding.appListAlignmentSelectLayout.visibility = View.VISIBLE
            R.id.appListAlignmentLeft -> updateAppListAlignment(Gravity.START)
            R.id.appListAlignmentCenter -> updateAppListAlignment(Gravity.CENTER)
            R.id.appListAlignmentRight -> updateAppListAlignment(Gravity.END)
            R.id.clockAlignment -> binding.clockAlignmentSelectLayout.visibility = View.VISIBLE
            R.id.clockAlignmentLeft -> viewModel.updateClockAlignment(Gravity.START)
            R.id.clockAlignmentCenter -> viewModel.updateClockAlignment(Gravity.CENTER)
            R.id.clockAlignmentRight -> viewModel.updateClockAlignment(Gravity.END)
            R.id.weatherEnabled -> toggleWeatherEnabled()
            R.id.weatherShowLocation -> toggleWeatherShowLocation()
            R.id.weatherUseDeviceLocation -> toggleWeatherLocationMode()
            R.id.weatherLocation -> openWeatherLocationPicker()
            R.id.weatherApp -> showAppListIfEnabled(Constants.FLAG_SET_WEATHER_APP)
            R.id.weatherSide -> binding.weatherSideSelectLayout.visibility = View.VISIBLE
            R.id.weatherSideTop -> updateWeatherSide(Constants.WeatherSide.TOP)
            R.id.weatherSideBottom -> updateWeatherSide(Constants.WeatherSide.BOTTOM)
            R.id.weatherUnits -> binding.weatherUnitsSelectLayout.visibility = View.VISIBLE
            R.id.weatherUnitsCelsius -> updateWeatherUnits(Constants.WeatherUnits.CELSIUS)
            R.id.weatherUnitsFahrenheit -> updateWeatherUnits(Constants.WeatherUnits.FAHRENHEIT)
            R.id.statusBar -> toggleStatusBar()
            R.id.rainbowMode -> toggleRainbowMode()
            R.id.dateTime -> binding.dateTimeSelectLayout.visibility = View.VISIBLE
            R.id.dateTimeOn -> toggleDateTime(Constants.DateTime.ON)
            R.id.dateTimeOff -> toggleDateTime(Constants.DateTime.OFF)
            R.id.dateOnly -> toggleDateTime(Constants.DateTime.DATE_ONLY)
            R.id.appThemeText -> binding.appThemeSelectLayout.visibility = View.VISIBLE
            R.id.themeLight -> updateTheme(AppCompatDelegate.MODE_NIGHT_NO)
            R.id.themeDark -> updateTheme(AppCompatDelegate.MODE_NIGHT_YES)
            R.id.themeSystem -> updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            R.id.textSizeValue -> binding.textSizesLayout.visibility = View.VISIBLE
            R.id.font -> binding.fontSelectLayout.visibility = View.VISIBLE
            R.id.fontSystem -> updateFont(Constants.FontFamily.SYSTEM)
            R.id.fontInter -> updateFont(Constants.FontFamily.INTER)
            R.id.fontJetBrainsMono -> updateFont(Constants.FontFamily.JETBRAINS_MONO)
            R.id.fontAtkinson -> updateFont(Constants.FontFamily.ATKINSON_HYPERLEGIBLE)
            R.id.fontOpenDyslexic -> updateFont(Constants.FontFamily.OPEN_DYSLEXIC)
            R.id.actionAccessibility -> openAccessibilityService()
            R.id.closeAccessibility -> toggleAccessibilityVisibility(false)
            R.id.notWorking -> requireContext().openUrl(Constants.URL_DOUBLE_TAP)

            R.id.tvGestures -> binding.flSwipeDown.visibility = View.VISIBLE

            R.id.textSizeMinus -> adjustTextSizePreview(-0.1f)
            R.id.textSizePlus -> adjustTextSizePreview(0.1f)

            R.id.swipeLeftApp -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_LEFT_APP)
            R.id.swipeRightApp -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_RIGHT_APP)
            R.id.swipeDownAction -> binding.swipeDownSelectLayout.visibility = View.VISIBLE
            R.id.notifications -> updateSwipeDownAction(Constants.SwipeDownAction.NOTIFICATIONS)
            R.id.search -> updateSwipeDownAction(Constants.SwipeDownAction.SEARCH)

            R.id.aboutOlauncher -> {
                prefs.aboutClicked = true
                requireContext().openUrl(Constants.URL_ABOUT_OLAUNCHER)
            }

            R.id.share -> requireActivity().shareApp()
            R.id.rate -> {
                prefs.rateClicked = true
                requireActivity().rateApp()
            }

            R.id.twitter -> requireContext().openUrl(Constants.URL_TWITTER_TANUJ)
            R.id.github -> requireContext().openUrl(Constants.URL_OLAUNCHER_GITHUB)
            R.id.privacy -> requireContext().openUrl(Constants.URL_OLAUNCHER_PRIVACY)
            R.id.footer -> {
                requireContext().openUrl(
                    if (showPentastic) Constants.URL_PENTASTIC else Constants.URL_NTS
                )
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.alignment -> {
                prefs.appLabelAlignment = prefs.homeAlignment
                findNavController().navigate(R.id.action_settingsFragment_to_appListFragment)
                requireContext().showToast(getString(R.string.alignment_changed))
            }

            R.id.appThemeText -> {
                binding.appThemeSelectLayout.visibility = View.VISIBLE
                binding.themeSystem.visibility = View.VISIBLE
            }

            R.id.swipeLeftApp -> toggleSwipeLeft()
            R.id.swipeRightApp -> toggleSwipeRight()
            R.id.toggleLock -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        return true
    }

    private fun initClickListeners() {
        binding.olauncherHiddenApps.setOnClickListener(this)
        binding.scrollLayout.setOnClickListener(this)
        binding.appInfo.setOnClickListener(this)
        binding.setLauncher.setOnClickListener(this)
        binding.aboutOlauncher.setOnClickListener(this)
        binding.autoShowKeyboard.setOnClickListener(this)
        binding.toggleLock.setOnClickListener(this)
        binding.homeButtonRecents.setOnClickListener(this)
        binding.appsNumMinus.setOnClickListener(this)
        binding.appsNumPlus.setOnClickListener(this)
        binding.screenTimeOnOff.setOnClickListener(this)
        binding.alignment.setOnClickListener(this)
        binding.alignmentLeft.setOnClickListener(this)
        binding.alignmentCenter.setOnClickListener(this)
        binding.alignmentRight.setOnClickListener(this)
        binding.homeBottomAlignment.setOnClickListener(this)
        binding.appListAlignment.setOnClickListener(this)
        binding.appListAlignmentLeft.setOnClickListener(this)
        binding.appListAlignmentCenter.setOnClickListener(this)
        binding.appListAlignmentRight.setOnClickListener(this)
        binding.clockAlignment.setOnClickListener(this)
        binding.clockAlignmentLeft.setOnClickListener(this)
        binding.clockAlignmentCenter.setOnClickListener(this)
        binding.clockAlignmentRight.setOnClickListener(this)
        binding.weatherEnabled.setOnClickListener(this)
        binding.weatherShowLocation.setOnClickListener(this)
        binding.weatherUseDeviceLocation.setOnClickListener(this)
        binding.weatherLocation.setOnClickListener(this)
        binding.weatherApp.setOnClickListener(this)
        binding.weatherSide.setOnClickListener(this)
        binding.weatherSideTop.setOnClickListener(this)
        binding.weatherSideBottom.setOnClickListener(this)
        binding.weatherUnits.setOnClickListener(this)
        binding.weatherUnitsCelsius.setOnClickListener(this)
        binding.weatherUnitsFahrenheit.setOnClickListener(this)
        binding.statusBar.setOnClickListener(this)
        binding.rainbowMode.setOnClickListener(this)
        binding.dateTime.setOnClickListener(this)
        binding.dateTimeOn.setOnClickListener(this)
        binding.dateTimeOff.setOnClickListener(this)
        binding.dateOnly.setOnClickListener(this)
        binding.swipeLeftApp.setOnClickListener(this)
        binding.swipeRightApp.setOnClickListener(this)
        binding.swipeDownAction.setOnClickListener(this)
        binding.search.setOnClickListener(this)
        binding.notifications.setOnClickListener(this)
        binding.appThemeText.setOnClickListener(this)
        binding.themeLight.setOnClickListener(this)
        binding.themeDark.setOnClickListener(this)
        binding.themeSystem.setOnClickListener(this)
        binding.textSizeValue.setOnClickListener(this)
        binding.font.setOnClickListener(this)
        binding.fontSystem.setOnClickListener(this)
        binding.fontInter.setOnClickListener(this)
        binding.fontJetBrainsMono.setOnClickListener(this)
        binding.fontAtkinson.setOnClickListener(this)
        binding.fontOpenDyslexic.setOnClickListener(this)
        binding.actionAccessibility.setOnClickListener(this)
        binding.closeAccessibility.setOnClickListener(this)
        binding.notWorking.setOnClickListener(this)

        binding.share.setOnClickListener(this)
        binding.rate.setOnClickListener(this)
        binding.twitter.setOnClickListener(this)
        binding.github.setOnClickListener(this)
        binding.privacy.setOnClickListener(this)
        binding.footer.setOnClickListener(this)

        binding.textSizeMinus.setOnClickListener(this)
        binding.textSizePlus.setOnClickListener(this)

        binding.alignment.setOnLongClickListener(this)
        binding.appThemeText.setOnLongClickListener(this)
        binding.swipeLeftApp.setOnLongClickListener(this)
        binding.swipeRightApp.setOnLongClickListener(this)
        binding.toggleLock.setOnLongClickListener(this)
    }

    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            viewModel.showDialog.postValue(Constants.Dialog.ABOUT)
            prefs.firstSettingsOpen = false
        }
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner) {
            if (it) {
                binding.setLauncher.text = getString(R.string.change_default_launcher)
                prefs.toShowHintCounter += 1
            }
        }
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            populateAlignment()
        }
        viewModel.clockAlignment.observe(viewLifecycleOwner) {
            populateAlignment()
        }
        viewModel.updateSwipeApps.observe(viewLifecycleOwner) {
            populateSwipeApps()
        }
    }

    private fun toggleSwipeLeft() {
        prefs.swipeLeftEnabled = !prefs.swipeLeftEnabled
        if (prefs.swipeLeftEnabled) {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast(getString(R.string.swipe_left_app_enabled))
        } else {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast(getString(R.string.swipe_left_app_disabled))
        }
    }

    private fun toggleSwipeRight() {
        prefs.swipeRightEnabled = !prefs.swipeRightEnabled
        if (prefs.swipeRightEnabled) {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast(getString(R.string.swipe_right_app_enabled))
        } else {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast(getString(R.string.swipe_right_app_disabled))
        }
    }

    private fun toggleStatusBar() {
        prefs.showStatusBar = !prefs.showStatusBar
        populateStatusBar()
    }

    private fun toggleRainbowMode() {
        prefs.rainbowMode = !prefs.rainbowMode
        populateRainbowMode()
        requireActivity().recreate()
    }

    private fun populateStatusBar() {
        if (prefs.showStatusBar) {
            showStatusBar()
            binding.statusBar.text = getString(R.string.on)
        } else {
            hideStatusBar()
            binding.statusBar.text = getString(R.string.off)
        }
    }

    private fun populateRainbowMode() {
        binding.rainbowMode.text = getString(if (prefs.rainbowMode) R.string.on else R.string.off)
    }

    private fun toggleDateTime(selected: Int) {
        prefs.dateTimeVisibility = selected
        populateDateTime()
        viewModel.toggleDateTime()
    }

    private fun populateDateTime() {
        binding.dateTime.text = getString(
            when (prefs.dateTimeVisibility) {
                Constants.DateTime.DATE_ONLY -> R.string.date
                Constants.DateTime.ON -> R.string.on
                else -> R.string.off
            }
        )
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun showHiddenApps() {
        if (prefs.hiddenApps.isEmpty()) {
            requireContext().showToast(getString(R.string.no_hidden_apps))
            return
        }
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to Constants.FLAG_HIDDEN_APPS)
        )
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
    }

    private fun toggleAccessibilityVisibility(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            binding.notWorking.visibility = View.VISIBLE
        if (isAccessServiceEnabled(requireContext()))
            binding.actionAccessibility.text = getString(R.string.disable)
        binding.accessibilityLayout.isVisible = show
        binding.scrollView.animateAlpha(if (show) 0.5f else 1f)
    }

    private fun openAccessibilityService() {
        toggleAccessibilityVisibility(false)
        // prefs.lockModeOn = true
        populateLockSettings()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun toggleLockMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!prefs.lockModeOn && !isAccessServiceEnabled(requireContext())) {
                toggleAccessibilityVisibility(true)
                return
            }
            prefs.lockModeOn = !prefs.lockModeOn
        } else {
            val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
            if (isAdmin) {
                removeActiveAdmin("Admin permission removed.")
                prefs.lockModeOn = false
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.admin_permission_message)
                )
                requireActivity().startActivityForResult(intent, Constants.REQUEST_CODE_ENABLE_ADMIN)
            }
        }
        populateLockSettings()
    }

    private fun removeActiveAdmin(toastMessage: String? = null) {
        try {
            deviceManager.removeActiveAdmin(componentName) // for backward compatibility
            requireContext().showToast(toastMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun changeHomeAppsNum(delta: Int) {
        val next = (prefs.homeAppsNum + delta).coerceIn(0, 12)
        if (next == prefs.homeAppsNum) return
        binding.homeAppsNum.text = next.toString()
        prefs.homeAppsNum = next
        viewModel.refreshHome(true)
    }

    private var pendingTextSizeScale: Float = -1f

    private fun adjustTextSizePreview(delta: Float) {
        val maxScale = if (isTablet(requireContext())) 2.0f else 1.5f
        val current = if (pendingTextSizeScale > 0) pendingTextSizeScale else prefs.textSizeScale
        val newScale = Math.round((current + delta) * 10f) / 10f
        val clamped = newScale.coerceIn(0.5f, maxScale)
        if (clamped == current) return
        pendingTextSizeScale = clamped
        val formatted = String.format("%.1f", clamped)
        binding.textSizeValue.text = formatted
        binding.textSizeCurrent.text = formatted
    }

    private fun applyTextSizeScale() {
        if (pendingTextSizeScale < 0 || prefs.textSizeScale == pendingTextSizeScale) {
            pendingTextSizeScale = -1f
            return
        }
        prefs.textSizeScale = pendingTextSizeScale
        pendingTextSizeScale = -1f
        requireActivity().recreate()
    }

    private fun toggleKeyboardText() {
        if (prefs.autoShowKeyboard && prefs.keyboardMessageShown.not()) {
            viewModel.showDialog.postValue(Constants.Dialog.KEYBOARD)
            prefs.keyboardMessageShown = true
        } else {
            prefs.autoShowKeyboard = !prefs.autoShowKeyboard
            populateKeyboardText()
        }
    }

    private fun updateTheme(appTheme: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == appTheme) return
        prefs.appTheme = appTheme
        populateAppThemeText(appTheme)
        setAppTheme(appTheme)
    }

    private fun setAppTheme(theme: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == theme) return
        requireActivity().recreate()
    }

    private fun populateAppThemeText(appTheme: Int = prefs.appTheme) {
        when (appTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> binding.appThemeText.text = getString(R.string.dark)
            AppCompatDelegate.MODE_NIGHT_NO -> binding.appThemeText.text = getString(R.string.light)
            else -> binding.appThemeText.text = getString(R.string.system_default)
        }
    }

    private fun populateFont() {
        binding.font.text = getString(fontFamilyLabel(prefs.fontFamily))
    }

    private fun fontFamilyLabel(fontFamily: Int): Int = when (fontFamily) {
        Constants.FontFamily.INTER -> R.string.font_inter
        Constants.FontFamily.JETBRAINS_MONO -> R.string.font_jetbrains_mono
        Constants.FontFamily.ATKINSON_HYPERLEGIBLE -> R.string.font_atkinson_hyperlegible
        Constants.FontFamily.OPEN_DYSLEXIC -> R.string.font_open_dyslexic
        else -> R.string.font_system
    }

    private fun updateFont(fontFamily: Int) {
        if (prefs.fontFamily == fontFamily) return
        prefs.fontFamily = fontFamily
        populateFont()
        requireActivity().recreate()
    }

    private fun populateTextSize() {
        val formatted = String.format("%.1f", prefs.textSizeScale)
        binding.textSizeValue.text = formatted
        binding.textSizeCurrent.text = formatted
    }

    private fun populateScreenTimeOnOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (requireContext().appUsagePermissionGranted()) binding.screenTimeOnOff.text = getString(R.string.on)
            else binding.screenTimeOnOff.text = getString(R.string.off)
        } else binding.screenTimeLayout.visibility = View.GONE
    }

    private fun toggleWeatherEnabled() {
        prefs.weatherEnabled = !prefs.weatherEnabled
        populateWeatherSettings()
        viewModel.refreshHome(false)
    }

    private fun toggleWeatherShowLocation() {
        prefs.weatherShowLocation = !prefs.weatherShowLocation
        populateWeatherSettings()
        viewModel.refreshHome(false)
    }

    private fun toggleWeatherLocationMode() {
        val enablingDeviceLocation = !prefs.weatherUseDeviceLocation
        if (enablingDeviceLocation && !hasCoarseLocationPermission()) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), weatherLocationPermissionCode)
            return
        }
        prefs.weatherUseDeviceLocation = enablingDeviceLocation
        populateWeatherSettings()
        viewModel.refreshHome(false)
    }

    private fun openWeatherLocationPicker() {
        if (prefs.weatherUseDeviceLocation) {
            requireContext().showToast(getString(R.string.weather_location_device))
            return
        }
        WeatherLocationPickerDialog.show(
            context = requireContext(),
            scope = viewLifecycleOwner.lifecycleScope,
            fontFamily = prefs.fontFamily
        ) { result ->
            prefs.weatherLocationName = result.displayLabel()
            prefs.weatherLatitude = result.latitude.toFloat()
            prefs.weatherLongitude = result.longitude.toFloat()
            populateWeatherSettings()
            viewModel.refreshHome(false)
        }
    }

    private fun updateWeatherSide(side: Int) {
        if (side != Constants.WeatherSide.TOP && side != Constants.WeatherSide.BOTTOM) return
        if (prefs.weatherSide == side) return
        prefs.weatherSide = side
        populateWeatherSettings()
        viewModel.refreshHome(false)
    }

    private fun updateWeatherUnits(units: Int) {
        if (prefs.weatherUnits == units) return
        prefs.weatherUnits = units
        populateWeatherSettings()
        viewModel.refreshHome(false)
    }

    private fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun populateWeatherSettings() {
        val effectiveSide = when (prefs.weatherSide) {
            Constants.WeatherSide.TOP, Constants.WeatherSide.BOTTOM -> prefs.weatherSide
            else -> Constants.WeatherSide.TOP
        }
        if (effectiveSide != prefs.weatherSide) {
            prefs.weatherSide = effectiveSide
        }
        binding.weatherEnabled.text = getString(if (prefs.weatherEnabled) R.string.on else R.string.off)
        binding.weatherShowLocation.text = getString(if (prefs.weatherShowLocation) R.string.on else R.string.off)
        binding.weatherUseDeviceLocation.text = getString(if (prefs.weatherUseDeviceLocation) R.string.on else R.string.off)
        binding.weatherLocation.text = when {
            prefs.weatherUseDeviceLocation -> getString(R.string.weather_location_device)
            prefs.weatherLocationName.isNotBlank() -> prefs.weatherLocationName
            else -> getString(R.string.weather_location_none)
        }
        binding.weatherApp.text = if (prefs.weatherAppName.isNotBlank()) prefs.weatherAppName else getString(R.string.none)
        binding.weatherSide.text = getString(
            when (effectiveSide) {
                Constants.WeatherSide.TOP -> R.string.top
                Constants.WeatherSide.BOTTOM -> R.string.bottom
                else -> R.string.top
            }
        )
        binding.weatherUnits.text = getString(
            if (prefs.weatherUnits == Constants.WeatherUnits.FAHRENHEIT) R.string.fahrenheit else R.string.celsius
        )
    }

    private fun populateKeyboardText() {
        if (prefs.autoShowKeyboard) binding.autoShowKeyboard.text = getString(R.string.on)
        else binding.autoShowKeyboard.text = getString(R.string.off)
    }

    private fun updateHomeBottomAlignment() {
        prefs.homeBottomAlignment = !prefs.homeBottomAlignment
        populateAlignment()
        viewModel.updateHomeAlignment(prefs.homeAlignment)
    }

    private fun updateAppListAlignment(alignment: Int) {
        prefs.appLabelAlignment = alignment
        populateAlignment()
    }

    private fun populateAlignment() {
        when (prefs.homeAlignment) {
            Gravity.START -> binding.alignment.text = getString(R.string.left)
            Gravity.CENTER -> binding.alignment.text = getString(R.string.center)
            Gravity.END -> binding.alignment.text = getString(R.string.right)
        }
        binding.homeBottomAlignment.text = getString(
            if (prefs.homeBottomAlignment) R.string.on else R.string.off
        )
        when (prefs.clockAlignment) {
            Gravity.START -> binding.clockAlignment.text = getString(R.string.left)
            Gravity.CENTER -> binding.clockAlignment.text = getString(R.string.center)
            Gravity.END -> binding.clockAlignment.text = getString(R.string.right)
        }
        when (prefs.appLabelAlignment) {
            Gravity.START -> binding.appListAlignment.text = getString(R.string.left)
            Gravity.CENTER -> binding.appListAlignment.text = getString(R.string.center)
            Gravity.END -> binding.appListAlignment.text = getString(R.string.right)
        }
    }

    private fun toggleHomeButtonRecents() {
        if (!prefs.homeButtonShowRecents && !isAccessServiceEnabled(requireContext())) {
            toggleAccessibilityVisibility(true)
            return
        }
        prefs.homeButtonShowRecents = !prefs.homeButtonShowRecents
        populateHomeButtonRecents()
    }

    private fun populateHomeButtonRecents() {
        binding.homeButtonRecents.text = getString(
            if (prefs.homeButtonShowRecents && isAccessServiceEnabled(requireContext())) R.string.on
            else R.string.off
        )
    }

    private fun populateLockSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.toggleLock.text = getString(
                if (prefs.lockModeOn && isAccessServiceEnabled(requireContext())) R.string.on
                else R.string.off
            )
        } else {
            binding.toggleLock.text = getString(
                if (prefs.lockModeOn) R.string.on
                else R.string.off
            )
        }
    }

    private fun populateSwipeDownAction() {
        binding.swipeDownAction.text = when (prefs.swipeDownAction) {
            Constants.SwipeDownAction.NOTIFICATIONS -> getString(R.string.notifications)
            else -> getString(R.string.search)
        }
    }

    private fun updateSwipeDownAction(swipeDownFor: Int) {
        if (prefs.swipeDownAction == swipeDownFor) return
        prefs.swipeDownAction = swipeDownFor
        populateSwipeDownAction()
    }

    private fun populateSwipeApps() {
        binding.swipeLeftApp.text = prefs.appNameSwipeLeft
        binding.swipeRightApp.text = prefs.appNameSwipeRight
        if (!prefs.swipeLeftEnabled)
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
        if (!prefs.swipeRightEnabled)
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
    }

//    private fun populateDigitalWellbeing() {
//        binding.digitalWellbeing.isVisible = requireContext().isPackageInstalled(Constants.DIGITAL_WELLBEING_PACKAGE_NAME).not()
//                && requireContext().isPackageInstalled(Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME).not()
//                && prefs.hideDigitalWellbeing.not()
//    }

    private fun showAppListIfEnabled(flag: Int) {
        if ((flag == Constants.FLAG_SET_SWIPE_LEFT_APP) and !prefs.swipeLeftEnabled) {
            requireContext().showToast(getString(R.string.long_press_to_enable))
            return
        }
        if ((flag == Constants.FLAG_SET_SWIPE_RIGHT_APP) and !prefs.swipeRightEnabled) {
            requireContext().showToast(getString(R.string.long_press_to_enable))
            return
        }
        viewModel.getAppList(true)
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to flag)
        )
    }

    private fun populateActionHints() {
        if (prefs.aboutClicked.not())
            binding.aboutOlauncher.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_info, 0)
        if (viewModel.isOlauncherDefault.value != true) return
        if (prefs.rateClicked.not() && prefs.toShowHintCounter > Constants.HINT_RATE_US && prefs.toShowHintCounter < Constants.HINT_RATE_US + 100)
            binding.rate.setCompoundDrawablesWithIntrinsicBounds(0, android.R.drawable.arrow_down_float, 0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        viewModel.checkForMessages.call()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == weatherLocationPermissionCode) {
            prefs.weatherUseDeviceLocation = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            populateWeatherSettings()
            viewModel.refreshHome(false)
        }
    }
}