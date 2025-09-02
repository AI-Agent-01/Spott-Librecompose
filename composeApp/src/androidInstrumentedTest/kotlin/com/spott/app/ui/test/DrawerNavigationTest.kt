package com.spott.app.ui.test

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spott.app.SpottApp
import com.spott.app.SpottAppViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawerNavigationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun drawerOpensViaMenuIcon() {
        composeTestRule.setContent {
            SpottApp(
                windowSizeClass = WindowSizeClass.calculateFromSize(
                    DpSize(400.dp, 800.dp) // Compact width
                )
            )
        }
        
        // Verify drawer is initially closed
        composeTestRule.onNodeWithText("Find Parking").assertIsNotDisplayed()
        
        // Open drawer via menu icon
        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        
        // Verify drawer is now open
        composeTestRule.onNodeWithText("Find Parking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Bookings").assertIsDisplayed()
    }
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun drawerDoesNotContainSearchItem() {
        composeTestRule.setContent {
            SpottApp(
                windowSizeClass = WindowSizeClass.calculateFromSize(
                    DpSize(400.dp, 800.dp)
                )
            )
        }
        
        // Open drawer
        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        
        // Verify Search is NOT in the drawer
        composeTestRule.onNodeWithText("Search").assertDoesNotExist()
        composeTestRule.onNodeWithText("Search destination").assertDoesNotExist()
        
        // Verify other expected items are present
        composeTestRule.onNodeWithText("Find Parking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Bookings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile & Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun tappingRecentBookingsNavigatesAndShowsTabs() {
        composeTestRule.setContent {
            SpottApp(
                windowSizeClass = WindowSizeClass.calculateFromSize(
                    DpSize(400.dp, 800.dp)
                )
            )
        }
        
        // Open drawer
        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        
        // Click Recent Bookings
        composeTestRule.onNodeWithText("Recent Bookings").performClick()
        
        // Verify we're on the Bookings screen (tabs are visible)
        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Past").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active bookings will appear here").assertIsDisplayed()
    }
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun activeSessionVisibilityDependsOnHasActiveBooking() {
        var vm: SpottAppViewModel? = null
        
        composeTestRule.setContent {
            vm = viewModel<SpottAppViewModel>()
            SpottApp(
                windowSizeClass = WindowSizeClass.calculateFromSize(
                    DpSize(400.dp, 800.dp)
                )
            )
        }
        
        // Open drawer
        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        
        // Initially, Active Session should not be visible (hasActiveBooking = false)
        composeTestRule.onNodeWithText("Active Session").assertDoesNotExist()
        
        // Set hasActiveBooking to true
        composeTestRule.runOnUiThread {
            vm?.setActiveBooking(true)
        }
        
        // Now Active Session should be visible
        composeTestRule.onNodeWithText("Active Session").assertIsDisplayed()
        
        // Set back to false
        composeTestRule.runOnUiThread {
            vm?.setActiveBooking(false)
        }
        
        // Active Session should be hidden again
        composeTestRule.onNodeWithText("Active Session").assertDoesNotExist()
    }
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun hostItemsVisibilityDependsOnIsHostUser() {
        var vm: SpottAppViewModel? = null
        
        composeTestRule.setContent {
            vm = viewModel<SpottAppViewModel>()
            SpottApp(
                windowSizeClass = WindowSizeClass.calculateFromSize(
                    DpSize(400.dp, 800.dp)
                )
            )
        }
        
        // Open drawer
        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        
        // Initially, user is not a host (isHostUser = false)
        composeTestRule.onNodeWithText("Become a Host").assertIsDisplayed()
        composeTestRule.onNodeWithText("Host Hub").assertDoesNotExist()
        
        // Set isHostUser to true
        composeTestRule.runOnUiThread {
            vm?.setHost(true)
        }
        
        // Now Host Hub should be visible, Become a Host should be hidden
        composeTestRule.onNodeWithText("Host Hub").assertIsDisplayed()
        composeTestRule.onNodeWithText("Become a Host").assertDoesNotExist()
        
        // Set back to false
        composeTestRule.runOnUiThread {
            vm?.setHost(false)
        }
        
        // Should revert to original state
        composeTestRule.onNodeWithText("Become a Host").assertIsDisplayed()
        composeTestRule.onNodeWithText("Host Hub").assertDoesNotExist()
    }
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun searchScreenAccessibleOnlyFromFindParking() {
        composeTestRule.setContent {
            SpottApp(
                windowSizeClass = WindowSizeClass.calculateFromSize(
                    DpSize(400.dp, 800.dp)
                )
            )
        }
        
        // Start on Find Parking (home)
        composeTestRule.onNodeWithText("Find Parking (Map)").assertIsDisplayed()
        
        // Click the Search destination button
        composeTestRule.onNodeWithText("Search destination").performClick()
        
        // Verify we're on the Search screen
        composeTestRule.onNodeWithText("Search").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search for a destination").assertIsDisplayed()
        
        // Navigate back
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Verify we're back on Find Parking
        composeTestRule.onNodeWithText("Find Parking (Map)").assertIsDisplayed()
    }
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun permanentDrawerOnExpandedScreen() {
        composeTestRule.setContent {
            SpottApp(
                windowSizeClass = WindowSizeClass.calculateFromSize(
                    DpSize(1280.dp, 800.dp) // Expanded width
                )
            )
        }
        
        // On expanded screen, drawer items should be immediately visible
        // (permanent drawer, no need to open)
        composeTestRule.onNodeWithText("Find Parking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Bookings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile & Account").assertIsDisplayed()
        
        // Menu icon should not exist on expanded screens
        composeTestRule.onNodeWithContentDescription("Open navigation menu").assertDoesNotExist()
    }
}