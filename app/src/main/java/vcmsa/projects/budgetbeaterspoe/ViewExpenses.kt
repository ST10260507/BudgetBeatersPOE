package vcmsa.projects.budgetbeaterspoe

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ViewExpenses : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FilteredExpenseAdapter
    private lateinit var fromDateInput: EditText
    private lateinit var toDateInput: EditText
    private var allExpenses = listOf<ExpenseEntity>()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_expenses)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        setupViews()
        setupDatePickers()
        setupRecyclerView()
        setupButtons()
        loadAllExpenses()
        setupBottomNav()
    }

    private fun setupViews() {
        fromDateInput = findViewById(R.id.FromDateInput)
        toDateInput = findViewById(R.id.ToDateInput)
        recyclerView = findViewById(R.id.expensesRecyclerView)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FilteredExpenseAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun setupDatePickers() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val picker = { editText: EditText ->
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    editText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        fromDateInput.setOnClickListener { picker(fromDateInput) }
        toDateInput.setOnClickListener { picker(toDateInput) }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.submitBtn).setOnClickListener {
            val start = fromDateInput.text.toString()
            val end = toDateInput.text.toString()

            if (start.isEmpty() || end.isEmpty()) {
                Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (start > end) {
                Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            filterExpenses(start, end)
        }

        findViewById<Button>(R.id.ViewAllBtn).setOnClickListener {
            showAllExpenses()
        }
    }

    // Load all expenses from Firestore
    private fun loadAllExpenses() {
        db.collection("expenses")
            .get()
            .addOnSuccessListener { result ->
                val tempList = mutableListOf<ExpenseEntity>()
                for (doc in result) {
                    val expense = doc.toObject(ExpenseEntity::class.java)
                    tempList.add(expense)
                }
                allExpenses = tempList
                adapter = FilteredExpenseAdapter(allExpenses)
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Filter expenses in-memory based on date range
    private fun filterExpenses(start: String, end: String) {
        try {
            val inputFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = inputFmt.parse(start)!!
            val endDate = inputFmt.parse(end)!!

            val filtered = allExpenses.filter { exp ->
                try {
                    val expDate = inputFmt.parse(exp.date)!!
                    expDate.time in startDate.time..endDate.time
                } catch (ex: Exception) {
                    false
                }
            }

            if (filtered.isEmpty()) {
                Toast.makeText(this, "No expenses in selected range", Toast.LENGTH_SHORT).show()
            }

            adapter = FilteredExpenseAdapter(filtered)
            recyclerView.adapter = adapter
        } catch (e: Exception) {
            Toast.makeText(this, "Error filtering expenses: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAllExpenses() {
        fromDateInput.text.clear()
        toDateInput.text.clear()
        adapter = FilteredExpenseAdapter(allExpenses)
        recyclerView.adapter = adapter
    }

    private fun setupBottomNav() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Logout -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LogoutFragment())
                        .commit()
                    true
                }
                R.id.Menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Menu_NavFragment())
                        .commit()
                    true
                }
                R.id.BudgetingGuides -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, BudgetingGuidesFragment())
                        .commit()
                    true
                }
                R.id.Awards -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AwardsFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
