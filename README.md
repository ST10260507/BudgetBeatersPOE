# Budget Beaters

Budget Beaters is a user-friendly financial tracking Android app designed to help users manage and monitor their daily expenses, categorize spending, visualize finances using interactive charts, track progress through dashboards, and earn badges for reaching financial milestones. Its primary purpose is to empower users with effective tools for personal finance management, providing clear insights into their spending habits and helping them achieve financial goals.

Developed by:

-   Joshua de Wet - ST10313014
-   Ankriya Padayachee - ST10260507
-   Kyle Govender - ST10145498
-   Teagan Griffiths - ST10300913

GitHub Repository: [Budget Beaters] (https://github.com/VCWVL/prog7313-poe-ST10260507.git)

                - The default branch is: Ankri

    
Backup Repository: https://github.com/ST10260507/BudgetBeatersPOE.git

YouTube Video Link: [Demo Video] (https://youtu.be/-dxC40N69CY)

Build APK: https://github.com/ST10145498/Prog7313_BuildAPK_Final.githttps://github.com/ST10145498/Prog7313_BuildAPK_Final.git

## Key Features

-   **Dashboard & Analytics:** Provides an overview of daily spending, progress charts, and income categorized using dynamic pie and bar graphs.
-   **Expense Tracking:** Allows users to add, remove, and view detailed expense history, including descriptions, categories, and optional images.
-   **Category Management:** Enables the creation and management of income/expense categories, including setting and tracking financial goals.
-   **Badges & Awards:** Rewards users for achieving financial goals and staying within their budget.
-   **Shared Budgeting:** Facilitates collaboration by allowing multiple users to manage a budget together.
-   **Account & Navigation:** Features secure registration/login, password reset functionality, and a modern, intuitive swipe-based user interface.
-   **Info Centre:** Offers access to budgeting guides and financial tips.
-   **Exportable Reports:** Generates reports in both PDF and CSV formats.
-   **Budget Pie Chart:** Visually represents spending across categories.

## Additional Features

-   **Shared Budgeting:** Enables multiple users to collaborate on a single budget, facilitating the management of shared expenses and group savings goals.
-   **Exportable Reports:** Allows users to export financial data in PDF and CSV formats for analysis and sharing. These reports provide detailed summaries of income and expenses, aiding in financial planning and review.
-   **Budgeting Guide:** Provides comprehensive tutorials and tips on effective budgeting techniques, financial planning, and strategies for saving money. 
-   **Budget Pie Chart:** Offers a clear visual representation of spending distribution across different categories, making it easy to understand where money is going.

## Technologies & Dependencies

Built with Java/Kotlin in Android Studio, leveraging **Google Firebase** for backend services, and utilizing the following libraries:

-   **Google Firebase:**
    -   **Firebase Authentication:** For user registration, login, and password management.
    -   **Cloud Firestore:** For secure, scalable, and real-time cloud-based data storage (expenses, categories, user profiles).
    -   *Other Firebase services (if used, e.g., Storage for images, Analytics for insights):* (Please specify if you use other Firebase services like Storage for images or Analytics)
-   AndroidX Core, AppCompat, ConstraintLayout, Lifecycle
-   Navigation Components (fragment.ktx, ui.ktx)
-   MPAndroidChart for charting (`implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")`)
-   Glide for image handling (`implementation("com.github.bumptech.glide:glide:4.12.0")`, `kapt("com.github.bumptech.glide:compiler:4.12.0")`)
-   Espresso & JUnit for testing

## Functional Requirements

1.  Register and login user accounts (via Firebase Authentication)
2.  Reset forgotten passwords (via Firebase Authentication)
3.  Add, edit, and delete expense entries (stored in Cloud Firestore)
4.  View expenses filtered by date or category (data fetched from Cloud Firestore)
5.  Track daily spending with bar charts (data fetched from Cloud Firestore)
6.  Visualize budget status with pie charts (under, near, or over budget) (data fetched from Cloud Firestore)
7.  Manage categories with goals and descriptions (stored in Cloud Firestore)
8.  Earn and view badges/awards based on usage
9.  Share budgets with other users (data managed in Cloud Firestore)
10. Access app usage guides and financial tips

## Non-Functional Requirements

-   **Usability:** Intuitive user interface with swiping gestures and a clean navigation bar.
-   **Performance:** Efficient data retrieval from Cloud Firestore and smooth chart rendering.
-   **Scalability:** Designed to support multiple users and shared budgeting scenarios, leveraging Firebase's scalable infrastructure.
-   **Security:** Robust user authentication and secure data storage provided by Firebase Authentication and Cloud Firestore security rules.
-   **Reliability:** Cloud-based data storage ensures data persistence and availability, with Firebase's offline capabilities for continuous access.
-   **Maintainability:** Modular codebase using modern Android architecture (MVVM, Navigation Components).

## Design Considerations

The application's design prioritizes a user-friendly and intuitive experience. Key design considerations include:

* **Clean and Modern UI:** A visually appealing interface with a modern design using swipe gestures and a clear navigation bar for easy access to all features.
* **Intuitive Navigation:** Streamlined flow between screens, particularly through the use of a persistent bottom navigation bar and clear entry points for various functionalities.
* **Data Visualization:** Effective use of dynamic pie and bar charts (`MPAndroidChart`) to present complex financial data in an easily digestible format, aiding users in understanding their spending patterns.
* **User Feedback:** Implementation of dialogs for confirmations (e.g., deletions) and user-friendly input methods like date pickers and dropdowns.
* **Robust Data Handling:** Transition to Firebase Authentication and Cloud Firestore for secure, scalable, and reliable backend operations, ensuring data integrity and availability.

## GitHub and GitHub Actions

This project utilizes GitHub for version control and collaborative development. **GitHub Actions** are integrated to automate the build process, ensuring code quality and efficient deployment.

Specifically, GitHub Actions are used to:

* **Automated Builds:** Continuously build the Android application whenever changes are pushed to the repository, ensuring that the codebase remains compilable and functional. This helps in catching integration issues early.
* **Continuous Integration:** Facilitate a robust continuous integration pipeline, allowing for faster feedback on code changes and improved team collaboration.

**The image showing the Workflows is in the Word Document**

## Installation Instructions

    1.  **Firebase Project Setup:**
        * Go to the [Firebase Console](https://console.firebase.google.com/) and create a new Firebase project.
        * Add an Android app to your Firebase project.
        * Follow the instructions to download your `google-services.json` file and place it in your app module directory (`app/google-services.json`).
        * Ensure your project-level and app-level `build.gradle` files are configured for Firebase.
    
    2.  Clone the repository: `git clone https://github.com/VCWVL/prog7313-poe-ST10260507.git`
    3.  Open in Android Studio
    4.  Sync Gradle to install all dependencies
    5.  Build and run on an emulator or Android device

    **Running the App:**

    *Option A: On an Android Emulator*

        1.  Click on AVD Manager (top-right toolbar).
        2.  Create a new virtual device (Pixel 5 or any other).
        3.  Select an API level (preferably 30 or above).
        4.  Start the emulator.
        5.  Click the Run button or press Shift + F10 to deploy the app.

    *Option B: On a Physical Android Device*

        1.  Enable Developer Options and USB Debugging on your Android phone.
        2.  Connect your phone via USB.
        3.  Select your device in the Run target list.
        4.  Run the app.

## App Functionality (Feature Overview)

### Visual Assets

**The final app icon for Budget Beaters is our distinctive logo, designed to be recognizable and reflect the app's financial theme. Other image assets used throughout the application include various icons for categories, badges, and general user interface elements, all chosen to enhance the user experience and visual appeal.**

Picture of Final App Icon:

    https://github.com/user-attachments/assets/046c2f29-0bf7-4123-bfc4-fe9aabe2397a

### User Authentication & Onboarding

-   **Main Page:**

                Users swipe to enter.
  
-   **Registration Page:**
  
            -   Input: Name, Email, Password, Confirm Password.
            -   Action: Tap Sign Up to create account (handled by Firebase Authentication).
            -   Link: Already registered? Login here.
-   **Login Page:**
  
            -   Input: Email & Password.
            -   Link: Forgot Password (navigates to the reset page).
-   **Forgot Password Page:**
  
            -   Input: Email address.
            -   Action: Submit to receive reset instructions (handled by Firebase Authentication).

### Navigation & Menu Options

    -   Upon login, users are directed to the Menu Page.
    -   The navigation bar provides access to:
        -   Logout
        -   Information Details
        -   View Awards/Badges
        -   Return to Main Menu

### Pie Chart / Graph View

    -   Features:
        -   Displays a dynamic pie chart of financial categories.
        -   Includes a legend with color codes and category percentages.
    -   Add Category:
        -   Input: Name, optional description, minimum and maximum income goals.
        -   Action: Tap Save to add to the chart (data stored in Cloud Firestore).
    -   Delete Category:
        -   Select a category using a tick box.
        -   Confirm deletion in a dialog (data updated in Cloud Firestore).

### View & Manage Expenses

    -   Expense Management Features:
        -   Add Expense
        -   Remove Expense
        -   View All Expenses
        -   View Income by Category
    -   Add Expense:
        -   Input: Name, Amount, Date (using a date picker), Description, Category (selected from a dropdown).
        -   Optional: Add an image.
        -   Action: Tap Save (data stored in Cloud Firestore).
    -   Remove Expense:
        -   Select an expense from a list.
        -   Details are shown in a pop-up.
        -   Confirm deletion in the pop-up (data updated in Cloud Firestore).
    -   View All Expenses:
        -   Filter Options: Select a date range (From Date and To Date), or view all.
        -   Displays filtered expenses in a RecyclerView (data fetched from Cloud Firestore).
    -   View Income by Category:
        -   Select From Date, To Date, and a Category.
        -   Tap Submit to view filtered income stats (data fetched from Cloud Firestore).

### Daily Spending

    -   Input: Select a date range (From Date and To Date) or view all.
    -   Display: A bar graph illustrating spending trends over time (data fetched from Cloud Firestore).
    -   Purpose: To help users monitor their spending against their budget.

### Progress Dashboard

    -   Select a month to view a pie chart indicating budget status:
        -   Green: Under Budget
        -   Yellow: Near Limit
        -   Red: Overspent
    -   Categories are labeled with percentages.
    -   Option to export pie chart data.

### Shared Budgeting

    -   Add Members:
        -   Input: Number of members, names, and emails.
        -   Action: Tap Submit to invite/add users (data managed in Cloud Firestore).

### Categories Page

    -   Displays all financial categories (data fetched from Cloud Firestore).
    -   Double-click a category to view related expenses (data fetched from Cloud Firestore).

### Information Details

    -   Provides access to guides and tutorials:
        -   How to use the app
        -   Financial management tips

### Awards & Badges

    -   Displays milestones and achievements, such as:
        -   Stayed Under Budget for a Week
        -   Completed 1st Month Without Overspending
        -   Beat Your Budget for 6 Months
        -   Reduced Spending in 3 or More Categories
        -   Paid Off a Major Debt or Loan
        -   Used Public Transport or Carpooling to Save Money
        -   Logged All Expenses Correctly for 3+ Months
        -   Hit All Major Savings Goals for the Year

## Changelog

**Screen 2: Register page**

-   The font and textboxes have been adjusted for better compatibility with Android Studio.
-   The Login link has been changed to a button for improved user experience.

**Screen 3: Forgot Password**

-   The password reset process now leverages Firebase Authentication's built-in functionality for sending reset emails.

**Screen 4: Menu**

-   The bottom row of buttons has been replaced with a navigation bar for easier navigation.
-   A "Menu" button has been added to the navigation bar for easy access from any page.
-   The button color scheme has been updated to a mint green, while maintaining the overall color scheme.

**Screen 5: Budget Pie Chart**

-   The color scheme has been updated, but the overall design remains the same.
-   The pie chart dynamically updates as users add and delete categories.

**Screen 6: Add A Category**

-   Two new fields have been added, allowing users to set minimum and maximum income goals for each category.

**Screen 7: Remove a Category**

-   The design remains largely the same, but the symbols next to each category have been removed.

**Screen 8: View All Expenses**

-   This page now includes four buttons: "Add Expense", "Remove Expense", "View Expenses", and "View Income by Categories".
-   Filtering between pages has been streamlined.

**Screen 9: Categories**

-   This is now a dedicated page, accessible from the main menu.
-   Filtering is handled on this page.

**Screen 10: View Expense page**

-   This is now a dedicated page, accessible from the "View All Expenses" page.
-   The bottom buttons have been removed, and a filter has been added.
-   Filtered expenses are displayed in a RecyclerView.

**Screen 11: Add an Expense**

-   The buttons have been removed.
-   A date picker has been implemented for user-friendly date selection.
-   Categories are now selected from a dropdown list.
-   The back button has been removed, as the navigation bar provides access to the menu.

**Screen 12: Remove an Expense**

-   Instead of a dropdown, all expenses are displayed. Clicking an expense shows details in a pop-up, where deletion can be confirmed.
-   The "No" and "Yes" buttons have been removed, as the pop-up provides these options.

**Screen 13: View Category Income**

-   This new page allows users to view the total amount spent in a specific category during a certain time period.

**Screen 14 and 15: View Daily Spending**

-   The graph and filter are now combined on a single page.
-   The "View All Daily Spending" button has been removed.

## Troubleshooting Tips

-   **Firebase Connection Issues:** Ensure your `google-services.json` file is correctly placed and that your app has internet access. Check Firebase console for project setup errors.
-   **Gradle Build Issues:** Go to File > Invalidate Caches / Restart if the project fails to sync.
-   **Missing Dependencies:** Ensure a stable internet connection to download libraries.
-   **Emulator Errors:** Make sure Intel HAXM is installed or try using a different device image.

## References

-   Android Developers. (2019). Accessing data using Room DAOs | Android Developers. \[online] Available at: \[https://developer.android.com/training/data-storage/room/accessing-data].
-   Android Developers. (2020). Defining data using Room entities | Android Developers. \[online] Available at: \[https://developer.android.com/training/data-storage/room/defining-data].
-   Android Developers. (2024). Download Android Studio & App Tools - Android Developers. \[online] Available at: \[https://developer.android.com/studio?gad\_source=1&gbraid=0AAAAAC-IOZkfL\_U9h3SgaOdf\_qoQ354r\_&gclid=Cj0KCQjw2tHABhCiARIsANZzDWrLaqknO6MVmZcqj6OAJeQSHLVCEgMHc1gtjDrF2NrzdAWlQtxYYdQaAovVEALw\_wcB&gclsrc=aw.ds] \[Accessed 2 May 2025].
-   Android Developers. (n.d.). Fragment. \[online] Available at: \[https://developer.android.com/reference/android/app/Fragment].
-   Android Developers. (2025). Generated binding classes. \[online] Available at: \[https://developer.android.com/topic/libraries/data-binding/generated-binding] \[Accessed 2 May 2025].
-   Dmitry Chernozubov (2020). Recycler view, power of asynchronous view holders creation. \[online] Medium. Available at: \[https://medium.com/@icesrgt/recycler-view-power-of-asynchronous-view-holders-creation-b3c9fe067702] \[Accessed 2 May 2025].
-   Firebase (2019). Documentation | Firebase. \[online] Firebase. Available at: \[https://firebase.google.com/docs].
-   freeCodeCamp.org. (2024). How to Use Git and GitHub – a Guide for Beginners and Experienced Developers. \[online] Available at: \[https://www.freecodecamp.org/news/guide-to-git-github-for-beginners-and-experienced-devs/].
-   GitHub (2024). GitHub Actions Documentation - GitHub Docs. \[online] docs.github.com. Available at: \[https://docs.github.com/en/actions].
-   GitHub (2025). GitHub. \[online] GitHub. Available at: \[https://github.com/].
-   Google (2019). Save data in a local database using Room | Android Developers. \[online] Android Developers. Available at: \[https://developer.android.com/training/data-storage/room].
-   hangyuan (2021). Understanding Java volatile visibility. \[online] Stack Overflow. Available at: \[https://stackoverflow.com/questions/68427434/understanding-java-volatile-visibility].
-   OpenAI (2025). ChatGPT. \[online] ChatGPT. Available at: \[https://chatgpt.com/].
-   stackOverflow (n.d.). Newest Questions. \[online] Stack Overflow. Available at: \[https://stackoverflow.com/questions/].
-   www.freecodecamp.org. (n.d.). freeCodeCamp.org. \[online] Available at: \[https://www.freecodecamp.org].
-   www.w3schools.com. (n.d.). Git Tutorial. \[online] Available at: \[https://www.w3schools.com/git/].
