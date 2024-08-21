package org.vaadin.example.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.vaadin.example.MainLayout;
import org.vaadin.example.model.Expense;
import org.vaadin.example.service.ExpenseService;
import org.vaadin.example.service.SessionService;
import org.vaadin.example.service.UserService;
import com.vaadin.flow.component.combobox.ComboBox;
import org.vaadin.example.model.Budget;
import org.vaadin.example.service.BudgetService;


@Route(value = "expense", layout = MainLayout.class)
public class ExpenseView extends VerticalLayout {

    private final Grid<Expense> grid = new Grid<>(Expense.class);
    private final TextField descriptionField = new TextField("Description");
    private final TextField amountField = new TextField("Amount");
    private final DatePicker datePicker = new DatePicker("Date");
    private final ComboBox<Budget> budgetComboBox = new ComboBox<>("Select Budget");


    private final ExpenseService expenseService;
    private final SessionService sessionService;
    private final UserService userService;
    private final BudgetService budgetService;

    private H2 totalExpensesValue;
    private Div totalExpensesCard;

    public ExpenseView(ExpenseService expenseService, SessionService sessionService, UserService userService, BudgetService budgetService) {
        this.expenseService = expenseService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.budgetService = budgetService;
    
        System.out.println("ExpenseView constructor called");
    
        configureGrid();
        configureForm();
    
        Button addButton = new Button("Add Expense", event -> addExpense());
        Button deleteButton = new Button("Delete Expense", event -> deleteExpense());
    
        HorizontalLayout formLayout = new HorizontalLayout(descriptionField, amountField, datePicker, budgetComboBox, addButton, deleteButton);
        formLayout.setWidthFull();
        formLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
    
        totalExpensesValue = new H2("$ 0.00");
        totalExpensesCard = createDashboardCard("Total Expenses for this Month", totalExpensesValue);
    
        H1 logo = new H1("Expenses");
    
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.add(logo, totalExpensesCard, formLayout, grid);
        mainLayout.setSpacing(false);
    
        add(mainLayout);
    
        listBudgets(); // Populate the budget dropdown
        listExpenses();
        updateTotalExpenses();
    }
    
    private void configureGrid() {
        grid.setColumns("description", "amount", "date");

        // Handle the budget column with a null-safe approach
        grid.addColumn(expense -> {
            Budget budget = expense.getBudget();
            return budget != null ? budget.getName() : "No Budget";
        }).setHeader("Budget");
    }

    private void configureForm() {
        descriptionField.setPlaceholder("e.g., Groceries");
        amountField.setPlaceholder("e.g., 200.00");
        datePicker.setPlaceholder("Select a date");
    }

    private void listBudgets() {
        Long userId = sessionService.getLoggedInUserId();
        List<Budget> budgets = budgetService.getBudgetsByUserId(userId);
        budgetComboBox.setItems(budgets);
        budgetComboBox.setItemLabelGenerator(Budget::getName);
    }

    private void listExpenses() {
        Long userId = sessionService.getLoggedInUserId();
        List<Expense> expenses = expenseService.getExpensesByUserId(userId);
        // Filter out or handle expenses with null budgets
        grid.setItems(expenses);
    }
    

    private void addExpense() {
        String description = descriptionField.getValue();
        String amountText = amountField.getValue();
        BigDecimal amount = new BigDecimal(amountText);
        LocalDate date = datePicker.getValue();
        Budget selectedBudget = budgetComboBox.getValue();

        Expense expense = new Expense();
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setDate(date != null ? java.sql.Date.valueOf(date) : null);
        expense.setUser(userService.findUserById(sessionService.getLoggedInUserId()));
        expense.setBudget(selectedBudget);

        expenseService.addExpense(expense);
        Notification.show("Expense added successfully");

        if (selectedBudget != null) {
            selectedBudget.setCurrentAmount(selectedBudget.getCurrentAmount().add(amount));
            budgetService.addBudget(selectedBudget);  // Update the budget's current amount
        }

        listExpenses();
        updateTotalExpenses();
        clearForm();
    }

   private void deleteExpense() {
        Expense selectedExpense = grid.asSingleSelect().getValue();
        if (selectedExpense != null) {
            Budget associatedBudget = selectedExpense.getBudget();
            if (associatedBudget != null) {
                associatedBudget.setCurrentAmount(associatedBudget.getCurrentAmount().subtract(selectedExpense.getAmount()));
                budgetService.addBudget(associatedBudget);  // Update the budget's current amount
            }

            expenseService.deleteExpense(selectedExpense.getId());
            Notification.show("Expense deleted successfully");
            listExpenses();
            updateTotalExpenses();
        } else {
            Notification.show("Please select an expense to delete");
        }
    }

    private void clearForm() {
        descriptionField.clear();
        amountField.clear();
        datePicker.clear();
        budgetComboBox.clear();
    }

    private Div createDashboardCard(String title, H2 valueComponent) {
        Div card = new Div();
        card.addClassName("dashboard-card");

        H3 cardTitle = new H3(title);
        cardTitle.addClassName("card-title");

        valueComponent.addClassName("card-value");

        card.add(cardTitle, valueComponent);
        return card;
    }

    private void updateTotalExpenses() {
        Long userId = sessionService.getLoggedInUserId();
        List<Expense> expenses = expenseService.getExpensesByUserId(userId);
        BigDecimal totalExpenses = expenses.stream()
                                           .map(Expense::getAmount)
                                           .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalExpensesValue.setText("$ " + totalExpenses.toString());
    }
}
