package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.service.FeeService;
import com.jenos.gestionecole.service.PaymentService;
import com.jenos.gestionecole.service.StaffService;
import com.jenos.gestionecole.service.StudentService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PaymentsController — Contrôleur unifié pour :
 * 1. Nouveau Paiement (Élève / Personnel) avec résumé et protection anti-double-clic
 * 2. Historique des Paiements avec filtres en cascade (Section -> Promotion -> Option) et totaux séparés USD/CDF
 * 3. Journal des Reçus & Duplicatas avec recherche et aperçu officiel
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément, JAMAIS additionnés.
 */
public class PaymentsController implements MoneyVisibilityManager.VisibilityObserver {

    // ── Onglets ──
    @FXML private TabPane tabPanePayments;

    // ── ONGLET 1 : Nouveau Paiement ──
    @FXML private Label lblReceiptNumber;
    @FXML private ToggleButton btnTypeEleve;
    @FXML private ToggleButton btnTypePersonnel;

    // Sélection Élève
    @FXML private VBox boxEleveSelection;
    @FXML private TextField txtStudentSearch;
    @FXML private Button btnSearchStudent;
    @FXML private TableView<Student> tblStudentResults;
    @FXML private TableColumn<Student, String> colMatricule;
    @FXML private TableColumn<Student, String> colNom;
    @FXML private TableColumn<Student, String> colClasse;
    @FXML private TableColumn<Student, String> colOption;

    @FXML private Label lblStudentName;
    @FXML private Label lblStudentMatricule;
    @FXML private Label lblStudentClass;
    @FXML private Label lblStudentOption;
    @FXML private Label lblTotalDue;
    @FXML private Label lblTotalPaid;
    @FXML private Label lblBalance;

    // Sélection Personnel
    @FXML private VBox boxPersonnelSelection;
    @FXML private ComboBox<Staff> cmbPersonnel;
    @FXML private ComboBox<Payment.StaffPaymentType> cmbStaffPaymentType;

    // Formulaire commun
    @FXML private Label lblPaymentError;
    @FXML private Label lblPaymentSuccess;
    @FXML private ComboBox<Fee> cmbFee;
    @FXML private TextField txtAmount;
    @FXML private ComboBox<String> cmbCurrency;
    @FXML private ComboBox<Payment.PaymentMethod> cmbPaymentMethod;
    @FXML private VBox boxReference;
    @FXML private TextField txtReference;
    @FXML private Label lblExpectedAmount;

    // Résumé avant validation
    @FXML private VBox boxPaymentSummary;
    @FXML private Label lblSummaryPerson;
    @FXML private Label lblSummaryFee;
    @FXML private Label lblSummaryAmount;
    @FXML private Label lblSummaryMethod;
    @FXML private Label lblSummaryCash;
    @FXML private Label lblSummaryWarning;

    @FXML private Button btnResetPayment;
    @FXML private Button btnValidatePayment;

    // ── ONGLET 2 : Historique des Paiements ──
    @FXML private DatePicker dpDateFrom;
    @FXML private DatePicker dpDateTo;
    @FXML private ComboBox<Section> cmbFilterSection;
    @FXML private ComboBox<Promotion> cmbFilterPromotion;
    @FXML private ComboBox<AcademicOption> cmbFilterOption;
    @FXML private ComboBox<String> cmbFilterCurrency;
    @FXML private ComboBox<Payment.PaymentStatus> cmbFilterStatus;
    @FXML private Button btnResetHistoryFilters;
    @FXML private Label lblTotalUSD;
    @FXML private Label lblTotalCDF;
    @FXML private Button btnToggleAmounts;
    @FXML private TableView<Payment> tblPayments;
    @FXML private TableColumn<Payment, String> tcPayDate;
    @FXML private TableColumn<Payment, String> tcReceiptNum;
    @FXML private TableColumn<Payment, String> tcPerson;
    @FXML private TableColumn<Payment, String> tcPayOption;
    @FXML private TableColumn<Payment, String> tcFeeType;
    @FXML private TableColumn<Payment, String> tcPayAmount;
    @FXML private TableColumn<Payment, String> tcPayCurrency;
    @FXML private TableColumn<Payment, String> tcPayMethod;
    @FXML private TableColumn<Payment, String> tcPayStatus;
    @FXML private TableColumn<Payment, String> tcCashier;
    @FXML private TableColumn<Payment, Void> tcPayActions;
    @FXML private Label lblHistoryInfo;

    // ── ONGLET 3 : Journal des Reçus ──
    @FXML private TextField txtSearchReceipt;
    @FXML private DatePicker dpReceiptDate;
    @FXML private Button btnRefreshReceipts;
    @FXML private TableView<Receipt> tblReceipts;
    @FXML private TableColumn<Receipt, String> colReceiptDate;
    @FXML private TableColumn<Receipt, String> colReceiptNumber;
    @FXML private TableColumn<Receipt, String> colReceiptBeneficiary;
    @FXML private TableColumn<Receipt, String> colReceiptAmount;
    @FXML private TableColumn<Receipt, String> colReceiptCurrency;
    @FXML private TableColumn<Receipt, String> colReceiptCashier;
    @FXML private TableColumn<Receipt, Void> colReceiptActions;
    @FXML private Label lblReceiptsInfo;

    // Services
    private final StudentService studentService = StudentService.getInstance();
    private final StaffService staffService = StaffService.getInstance();
    private final PaymentService paymentService = PaymentService.getInstance();
    private final AcademicStructureService academicService = AcademicStructureService.getInstance();

    private final ObservableList<Student> searchResults = FXCollections.observableArrayList();
    private final ObservableList<Payment> paymentsList = FXCollections.observableArrayList();
    private final ObservableList<Receipt> receiptsList = FXCollections.observableArrayList();

    private Student selectedStudent;
    private PaymentService.StudentFinancialSituation currentSituation;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ToggleGroup typeToggleGroup;

    @FXML
    public void initialize() {
        MoneyVisibilityManager.getInstance().registerObserver(this);

        setupTypeToggle();
        setupStudentSearchTable();
        setupPaymentForm();
        setupHistoryFilters();
        setupHistoryTable();
        setupReceiptJournalTable();

        refreshNextReceiptNumber();
        loadHistoryPayments();
        loadRecentReceipts();
    }

    private void refreshNextReceiptNumber() {
        if (lblReceiptNumber != null) {
            lblReceiptNumber.setText("Prochain N° : " + paymentService.findNextReceiptNumber());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ONGLET 1 : NOUVEAU PAIEMENT
    // ─────────────────────────────────────────────────────────────

    private void setupTypeToggle() {
        typeToggleGroup = new ToggleGroup();
        btnTypeEleve.setToggleGroup(typeToggleGroup);
        btnTypePersonnel.setToggleGroup(typeToggleGroup);
        btnTypeEleve.setSelected(true);

        typeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                // Empêcher la désélection complète
                btnTypeEleve.setSelected(true);
                return;
            }
            boolean isEleve = (newVal == btnTypeEleve);
            boxEleveSelection.setVisible(isEleve);
            boxEleveSelection.setManaged(isEleve);
            boxPersonnelSelection.setVisible(!isEleve);
            boxPersonnelSelection.setManaged(!isEleve);

            // Adapter la liste des frais
            if (isEleve) {
                if (selectedStudent != null) {
                    cmbFee.setItems(FXCollections.observableArrayList(paymentService.getFeesForStudent(selectedStudent)));
                } else {
                    cmbFee.setItems(FXCollections.observableArrayList(FeeService.getInstance().findAllActive()));
                }
            } else {
                cmbFee.setItems(FXCollections.observableArrayList());
            }

            updateSummaryAndValidation();
        });
    }

    private void setupStudentSearchTable() {
        colMatricule.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMatricule()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colClasse.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getClassName()));
        colOption.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOptionName()));

        tblStudentResults.setItems(searchResults);
        tblStudentResults.getSelectionModel().selectedItemProperty().addListener((obs, o, newStudent) -> {
            if (newStudent != null) {
                selectStudent(newStudent);
            }
        });
    }

    private void setupPaymentForm() {
        // Devises
        cmbCurrency.setItems(FXCollections.observableArrayList("USD", "CDF"));
        cmbCurrency.setValue("USD");

        // Modes de paiement
        cmbPaymentMethod.setItems(FXCollections.observableArrayList(Payment.PaymentMethod.values()));
        cmbPaymentMethod.setValue(Payment.PaymentMethod.ESPECES);

        // Personnel
        cmbPersonnel.setItems(FXCollections.observableArrayList(staffService.findAllActive()));
        cmbStaffPaymentType.setItems(FXCollections.observableArrayList(Payment.StaffPaymentType.values()));
        cmbStaffPaymentType.setValue(Payment.StaffPaymentType.SALAIRE);

        // Frais par défaut
        cmbFee.setItems(FXCollections.observableArrayList(FeeService.getInstance().findAllActive()));

        // Listeners pour mise à jour dynamique
        cmbPaymentMethod.valueProperty().addListener((obs, o, method) -> {
            boolean refRequired = (method == Payment.PaymentMethod.MOBILE_MONEY
                    || method == Payment.PaymentMethod.VIREMENT
                    || method == Payment.PaymentMethod.CHEQUE);
            boxReference.setVisible(refRequired);
            boxReference.setManaged(refRequired);
            updateSummaryAndValidation();
        });

        cmbFee.valueProperty().addListener((obs, o, fee) -> {
            if (fee != null) {
                txtAmount.setText(String.format("%.2f", fee.getAmount()).replace(',', '.'));
                cmbCurrency.setValue(fee.getCurrency());
                lblExpectedAmount.setText(fee.getAmount() + " " + fee.getCurrency());
            } else {
                lblExpectedAmount.setText("—");
            }
            updateSummaryAndValidation();
        });

        txtAmount.textProperty().addListener((obs, o, n) -> updateSummaryAndValidation());
        txtReference.textProperty().addListener((obs, o, n) -> updateSummaryAndValidation());
        cmbCurrency.valueProperty().addListener((obs, o, n) -> updateSummaryAndValidation());
        cmbPersonnel.valueProperty().addListener((obs, o, n) -> updateSummaryAndValidation());
        cmbStaffPaymentType.valueProperty().addListener((obs, o, n) -> updateSummaryAndValidation());
    }

    @FXML
    private void handleSearchStudent() {
        String kw = txtStudentSearch.getText() != null ? txtStudentSearch.getText().trim() : "";
        List<Student> results = studentService.searchByKeyword(kw);
        searchResults.setAll(results);
    }

    private void selectStudent(Student s) {
        this.selectedStudent = s;
        lblStudentName.setText(s.getFullName());
        lblStudentMatricule.setText("Matricule: " + s.getMatricule());
        lblStudentClass.setText(s.getClassName());
        lblStudentOption.setText(s.getOptionName());

        this.currentSituation = paymentService.getSituationForStudent(s);
        refreshFinancialDisplay();

        List<Fee> fees = paymentService.getFeesForStudent(s);
        cmbFee.setItems(FXCollections.observableArrayList(fees));
        if (!fees.isEmpty()) {
            cmbFee.setValue(fees.get(0));
        }

        updateSummaryAndValidation();
    }

    private void updateSummaryAndValidation() {
        boolean isEleve = btnTypeEleve.isSelected();
        boolean valid = true;
        String warning = "";

        // Personne
        String personName = "—";
        if (isEleve) {
            if (selectedStudent != null) {
                personName = selectedStudent.getFullName() + " (" + selectedStudent.getClassName() + ")";
            } else {
                valid = false;
            }
        } else {
            Staff st = cmbPersonnel.getValue();
            if (st != null) {
                personName = st.getFullName() + " (" + st.getDepartment() + ")";
            } else {
                valid = false;
            }
        }
        lblSummaryPerson.setText(personName);

        // Frais ou Motif
        String feeName = "—";
        if (isEleve) {
            Fee f = cmbFee.getValue();
            if (f != null) {
                feeName = f.getName();
            } else {
                valid = false;
            }
        } else {
            Payment.StaffPaymentType spt = cmbStaffPaymentType.getValue();
            if (spt != null) {
                feeName = "Règlement : " + spt.name();
            } else {
                valid = false;
            }
        }
        lblSummaryFee.setText(feeName);

        // Montant & Devise
        double amount = 0.0;
        String cur = cmbCurrency.getValue() != null ? cmbCurrency.getValue() : "USD";
        try {
            String raw = txtAmount.getText() != null ? txtAmount.getText().trim() : "";
            if (raw.isEmpty()) {
                valid = false;
            } else {
                amount = Double.parseDouble(raw);
                if (amount <= 0) {
                    valid = false;
                    warning = "Le montant doit être strictement supérieur à zéro.";
                }
            }
        } catch (NumberFormatException e) {
            valid = false;
            warning = "Format de montant invalide.";
        }
        lblSummaryAmount.setText(amount > 0 ? (amount + " " + cur) : "—");

        // Mode et Référence
        Payment.PaymentMethod method = cmbPaymentMethod.getValue();
        lblSummaryMethod.setText(method != null ? method.name() : "—");
        if (method == Payment.PaymentMethod.MOBILE_MONEY
                || method == Payment.PaymentMethod.VIREMENT
                || method == Payment.PaymentMethod.CHEQUE) {
            String ref = txtReference.getText() != null ? txtReference.getText().trim() : "";
            if (ref.isEmpty()) {
                valid = false;
                if (warning.isEmpty()) warning = "La référence de transaction est obligatoire pour ce mode.";
            }
        }

        // Caisse
        lblSummaryCash.setText("Caisse Principale (Jenos)");

        // Affichage du résumé
        boolean showSummary = (isEleve && selectedStudent != null) || (!isEleve && cmbPersonnel.getValue() != null);
        boxPaymentSummary.setVisible(showSummary);
        boxPaymentSummary.setManaged(showSummary);

        if (!warning.isEmpty()) {
            lblSummaryWarning.setText(warning);
            lblSummaryWarning.setVisible(true);
            lblSummaryWarning.setManaged(true);
        } else {
            lblSummaryWarning.setVisible(false);
            lblSummaryWarning.setManaged(false);
        }

        btnValidatePayment.setDisable(!valid);
    }

    @FXML
    private void handleSubmitPayment() {
        hideAlerts();

        // 1. Protection anti-double clic
        btnValidatePayment.setDisable(true);
        btnValidatePayment.setText("VALIDATION EN COURS...");

        try {
            boolean isEleve = btnTypeEleve.isSelected();
            double amount = Double.parseDouble(txtAmount.getText().trim());

            Payment p = new Payment();
            p.setAmount(amount);
            p.setCurrency(cmbCurrency.getValue());
            p.setPaymentMethod(cmbPaymentMethod.getValue());
            p.setReference(txtReference.getText() != null ? txtReference.getText().trim() : "");
            p.setPaymentDate(LocalDate.now());

            if (isEleve) {
                if (selectedStudent == null) {
                    showPaymentError("Veuillez sélectionner un élève.");
                    return;
                }
                p.setPaymentType(Payment.PaymentType.ELEVE);
                p.setStudent(selectedStudent);
                p.setFee(cmbFee.getValue());
            } else {
                Staff staff = cmbPersonnel.getValue();
                if (staff == null) {
                    showPaymentError("Veuillez sélectionner un membre du personnel.");
                    return;
                }
                p.setPaymentType(Payment.PaymentType.PERSONNEL);
                p.setStaff(staff);
                p.setStaffPaymentType(cmbStaffPaymentType.getValue());
            }

            // 2. Traitement métier via PaymentService
            Receipt receipt = paymentService.processPayment(p);

            // 3. Dialogue succès avec proposition d'aperçu / impression
            showPaymentSuccess("Paiement validé avec succès ! Quittance N° " + receipt.getReceiptNumber());

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Paiement Enregistré");
            successAlert.setHeaderText("Quittance N° " + receipt.getReceiptNumber() + " émise !");
            successAlert.setContentText("Le paiement de " + receipt.getAmountPaid() + " " + receipt.getCurrency()
                    + " a été validé et enregistré en caisse.\n\nSouhaitez-vous prévisualiser le reçu officiel ?");

            ButtonType btnVoirRecu = new ButtonType("👁 Aperçu du Reçu", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnFermer = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            successAlert.getButtonTypes().setAll(btnVoirRecu, btnFermer);

            successAlert.showAndWait().ifPresent(response -> {
                if (response == btnVoirRecu) {
                    paymentService.setLastGeneratedReceipt(receipt);
                    NavigationManager.getInstance().navigate(Route.RECEIPT_PREVIEW);
                }
            });

            // 4. Réinitialisation et rechargement
            handleResetPayment();
            loadHistoryPayments();
            loadRecentReceipts();
            refreshNextReceiptNumber();

        } catch (NumberFormatException e) {
            showPaymentError("Montant saisi invalide.");
        } catch (IllegalArgumentException e) {
            showPaymentError(e.getMessage());
        } catch (Exception e) {
            showPaymentError("Une erreur inattendue est survenue : " + e.getMessage());
        } finally {
            btnValidatePayment.setDisable(false);
            btnValidatePayment.setText("Valider le paiement");
            updateSummaryAndValidation();
        }
    }

    @FXML
    private void handleResetPayment() {
        txtAmount.clear();
        txtReference.clear();
        lblPaymentError.setVisible(false);
        lblPaymentError.setManaged(false);
        lblPaymentSuccess.setVisible(false);
        lblPaymentSuccess.setManaged(false);

        if (selectedStudent != null) {
            currentSituation = paymentService.getSituationForStudent(selectedStudent);
            refreshFinancialDisplay();
        }
        updateSummaryAndValidation();
    }

    private void showPaymentError(String msg) {
        lblPaymentError.setText(msg);
        lblPaymentError.setVisible(true);
        lblPaymentError.setManaged(true);
    }

    private void showPaymentSuccess(String msg) {
        lblPaymentSuccess.setText(msg);
        lblPaymentSuccess.setVisible(true);
        lblPaymentSuccess.setManaged(true);
    }

    private void hideAlerts() {
        lblPaymentError.setVisible(false);
        lblPaymentError.setManaged(false);
        lblPaymentSuccess.setVisible(false);
        lblPaymentSuccess.setManaged(false);
    }

    // ─────────────────────────────────────────────────────────────
    // ONGLET 2 : HISTORIQUE DES PAIEMENTS
    // ─────────────────────────────────────────────────────────────

    private void setupHistoryFilters() {
        // Sections
        cmbFilterSection.setItems(FXCollections.observableArrayList(academicService.findAllSections()));

        // Cascade Section -> Promotion
        cmbFilterSection.valueProperty().addListener((obs, o, section) -> {
            if (section != null) {
                cmbFilterPromotion.setItems(FXCollections.observableArrayList(academicService.findPromotionsBySection(section)));
            } else {
                cmbFilterPromotion.setItems(FXCollections.observableArrayList());
            }
            cmbFilterPromotion.setValue(null);
            cmbFilterOption.setValue(null);
            filterHistory();
        });

        // Cascade Promotion -> Option
        cmbFilterPromotion.valueProperty().addListener((obs, o, promo) -> {
            if (promo != null) {
                cmbFilterOption.setItems(FXCollections.observableArrayList(academicService.findOptionsByPromotion(promo)));
            } else {
                cmbFilterOption.setItems(FXCollections.observableArrayList());
            }
            cmbFilterOption.setValue(null);
            filterHistory();
        });

        cmbFilterOption.valueProperty().addListener((obs, o, n) -> filterHistory());

        // Devises
        cmbFilterCurrency.setItems(FXCollections.observableArrayList("Toutes", "USD", "CDF"));
        cmbFilterCurrency.setValue("Toutes");
        cmbFilterCurrency.valueProperty().addListener((obs, o, n) -> filterHistory());

        // Statuts
        cmbFilterStatus.setItems(FXCollections.observableArrayList(Payment.PaymentStatus.values()));
        cmbFilterStatus.valueProperty().addListener((obs, o, n) -> filterHistory());

        // Dates
        dpDateFrom.valueProperty().addListener((obs, o, n) -> filterHistory());
        dpDateTo.valueProperty().addListener((obs, o, n) -> filterHistory());
    }

    private void setupHistoryTable() {
        tcPayDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPaymentDate() != null ? c.getValue().getPaymentDate().format(df) : "—"));
        tcReceiptNum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReceiptNumber()));
        tcPerson.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStudent() != null ? c.getValue().getStudent().getFullName() :
                (c.getValue().getStaff() != null ? c.getValue().getStaff().getFullName() : "—")
        ));
        tcPayOption.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStudent() != null ? c.getValue().getStudent().getOptionName() : "N/A"
        ));
        tcFeeType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFee() != null ? c.getValue().getFee().getName() :
                (c.getValue().getStaffPaymentType() != null ? c.getValue().getStaffPaymentType().name() : "—")
        ));
        tcPayAmount.setCellValueFactory(c -> new SimpleStringProperty(
                MoneyFormatter.display(c.getValue().getAmount(), c.getValue().getCurrency())
        ));
        tcPayCurrency.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrency()));
        tcPayMethod.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPaymentMethod() != null ? c.getValue().getPaymentMethod().name() : "—"));
        tcPayStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() != null ? c.getValue().getStatus().name() : ""));
        tcCashier.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCashierUsername()));

        // Actions : Voir reçu & Annuler
        tcPayActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("👁");
            private final Button btnCancel = new Button("❌");
            private final HBox box = new HBox(6, btnView, btnCancel);

            {
                btnView.getStyleClass().add("btn-cell-action");
                btnCancel.getStyleClass().add("btn-cell-action");
                btnView.setTooltip(new Tooltip("Voir la quittance"));
                btnCancel.setTooltip(new Tooltip("Annuler le paiement"));
                box.setAlignment(Pos.CENTER);

                btnView.setOnAction(e -> {
                    Payment p = getTableView().getItems().get(getIndex());
                    if (p != null) {
                        Receipt r = paymentService.findReceiptByNumber(p.getReceiptNumber());
                        if (r != null) {
                            paymentService.setLastGeneratedReceipt(r);
                            NavigationManager.getInstance().navigate(Route.RECEIPT_PREVIEW);
                        }
                    }
                });

                btnCancel.setOnAction(e -> {
                    Payment p = getTableView().getItems().get(getIndex());
                    if (p != null) {
                        cancelPaymentConfirm(p);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Payment p = getTableView().getItems().get(getIndex());
                    boolean canCancel = RoleGuard.canCancelPayment() && p != null && p.getStatus() == Payment.PaymentStatus.VALIDEE;
                    btnCancel.setVisible(canCancel);
                    btnCancel.setManaged(canCancel);
                    setGraphic(box);
                }
            }
        });

        tblPayments.setItems(paymentsList);
    }

    private void cancelPaymentConfirm(Payment p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation d'annulation");
        confirm.setHeaderText("Annuler le paiement N° " + p.getReceiptNumber() + " ?");
        confirm.setContentText("Cette opération passera le paiement en statut ANNULÉE (non destructif).\n"
                + "Montant : " + p.getAmount() + " " + p.getCurrency() + "\n"
                + "Bénéficiaire : " + (p.getStudent() != null ? p.getStudent().getFullName() : p.getStaff().getFullName()));

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                boolean done = paymentService.cancelPayment(p.getId());
                if (done) {
                    loadHistoryPayments();
                }
            }
        });
    }

    private void loadHistoryPayments() {
        filterHistory();
    }

    private void filterHistory() {
        List<Payment> all = paymentService.findAllPayments();

        LocalDate from = dpDateFrom.getValue();
        LocalDate to = dpDateTo.getValue();
        Section sec = cmbFilterSection.getValue();
        Promotion promo = cmbFilterPromotion.getValue();
        AcademicOption opt = cmbFilterOption.getValue();
        String cur = cmbFilterCurrency.getValue();
        Payment.PaymentStatus status = cmbFilterStatus.getValue();

        List<Payment> filtered = all.stream().filter(p -> {
            if (from != null && p.getPaymentDate() != null && p.getPaymentDate().isBefore(from)) return false;
            if (to != null && p.getPaymentDate() != null && p.getPaymentDate().isAfter(to)) return false;
            if (status != null && p.getStatus() != status) return false;
            if (cur != null && !"Toutes".equalsIgnoreCase(cur) && !cur.equalsIgnoreCase(p.getCurrency())) return false;

            // Filtres hiérarchiques sur les élèves
            if (sec != null || promo != null || opt != null) {
                if (p.getStudent() == null) return false;
                Student s = p.getStudent();
                if (sec != null && (s.getSection() == null || s.getSection().getId() != sec.getId())) return false;
                if (promo != null && (s.getPromotion() == null || s.getPromotion().getId() != promo.getId())) return false;
                if (opt != null && (s.getOption() == null || s.getOption().getId() != opt.getId())) return false;
            }

            return true;
        }).collect(Collectors.toList());

        paymentsList.setAll(filtered);
        lblHistoryInfo.setText(filtered.size() + " paiement(s) trouvé(s)");

        // Calcul strict des totaux séparés USD et CDF (uniquement paiements VALIDEE)
        double sumUSD = 0.0;
        double sumCDF = 0.0;
        for (Payment p : filtered) {
            if (p.getStatus() == Payment.PaymentStatus.VALIDEE) {
                if ("USD".equalsIgnoreCase(p.getCurrency())) {
                    sumUSD += p.getAmount();
                } else if ("CDF".equalsIgnoreCase(p.getCurrency())) {
                    sumCDF += p.getAmount();
                }
            }
        }

        if (MoneyVisibilityManager.getInstance().isVisible()) {
            lblTotalUSD.setText(String.format("Total USD : %.2f USD", sumUSD));
            lblTotalCDF.setText(String.format("Total CDF : %,.0f CDF", sumCDF));
        } else {
            lblTotalUSD.setText("Total USD : " + MoneyFormatter.formatMasked());
            lblTotalCDF.setText("Total CDF : " + MoneyFormatter.formatMasked());
        }
    }

    @FXML
    private void handleResetHistoryFilters() {
        dpDateFrom.setValue(null);
        dpDateTo.setValue(null);
        cmbFilterSection.setValue(null);
        cmbFilterPromotion.setValue(null);
        cmbFilterOption.setValue(null);
        cmbFilterCurrency.setValue("Toutes");
        cmbFilterStatus.setValue(null);
        filterHistory();
    }

    @FXML
    private void handleToggleAmounts() {
        if (MoneyVisibilityManager.getInstance().isVisible()) {
            MoneyVisibilityManager.getInstance().hideAll();
        } else {
            MoneyVisibilityManager.getInstance().showAll();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ONGLET 3 : JOURNAL DES REÇUS & DUPLICATAS
    // ─────────────────────────────────────────────────────────────

    private void setupReceiptJournalTable() {
        colReceiptDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getIssuedAt() != null ? c.getValue().getIssuedAt().format(dtf) : "—"));
        colReceiptNumber.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReceiptNumber()));
        colReceiptBeneficiary.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBeneficiaryName() + " (" + c.getValue().getBeneficiaryDetail() + ")"));
        colReceiptAmount.setCellValueFactory(c -> new SimpleStringProperty(
                "USD".equalsIgnoreCase(c.getValue().getCurrency())
                        ? String.format("%.2f USD", c.getValue().getAmountPaid())
                        : String.format("%,.0f CDF", c.getValue().getAmountPaid())
        ));
        colReceiptCurrency.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrency()));
        colReceiptCashier.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCashierUsername()));

        colReceiptActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPrint = new Button("👁 Aperçu");
            private final Button btnDuplicate = new Button("📄 Duplicata");
            private final HBox box = new HBox(6, btnPrint, btnDuplicate);

            {
                btnPrint.getStyleClass().add("btn-secondary");
                btnDuplicate.getStyleClass().add("btn-secondary");
                box.setAlignment(Pos.CENTER);

                btnPrint.setOnAction(e -> {
                    Receipt r = getTableView().getItems().get(getIndex());
                    if (r != null) {
                        r.setDuplicate(false);
                        paymentService.setLastGeneratedReceipt(r);
                        NavigationManager.getInstance().navigate(Route.RECEIPT_PREVIEW);
                    }
                });

                btnDuplicate.setOnAction(e -> {
                    Receipt r = getTableView().getItems().get(getIndex());
                    if (r != null) {
                        r.setDuplicate(true);
                        paymentService.setLastGeneratedReceipt(r);
                        NavigationManager.getInstance().navigate(Route.RECEIPT_PREVIEW);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tblReceipts.setItems(receiptsList);

        txtSearchReceipt.textProperty().addListener((obs, o, n) -> searchReceipts());
        dpReceiptDate.valueProperty().addListener((obs, o, n) -> searchReceipts());
    }

    private void searchReceipts() {
        String kw = txtSearchReceipt.getText() != null ? txtSearchReceipt.getText().trim().toLowerCase() : "";
        LocalDate date = dpReceiptDate.getValue();

        List<Receipt> recs = paymentService.findRecentReceipts(100);
        List<Receipt> filtered = recs.stream().filter(r -> {
            if (!kw.isEmpty()) {
                boolean match = (r.getReceiptNumber() != null && r.getReceiptNumber().toLowerCase().contains(kw))
                        || (r.getBeneficiaryName() != null && r.getBeneficiaryName().toLowerCase().contains(kw));
                if (!match) return false;
            }
            if (date != null && r.getIssuedAt() != null) {
                if (!r.getIssuedAt().toLocalDate().isEqual(date)) return false;
            }
            return true;
        }).collect(Collectors.toList());

        receiptsList.setAll(filtered);
        lblReceiptsInfo.setText(filtered.size() + " reçu(s)");
    }

    private void loadRecentReceipts() {
        searchReceipts();
    }

    @FXML
    private void handleRefreshReceipts() {
        loadRecentReceipts();
    }

    // ─────────────────────────────────────────────────────────────
    // GESTION DE LA VISIBILITÉ DES MONTANTS
    // ─────────────────────────────────────────────────────────────

    private void refreshFinancialDisplay() {
        if (currentSituation == null) {
            lblTotalDue.setText(MoneyFormatter.formatMasked());
            lblTotalPaid.setText(MoneyFormatter.formatMasked());
            lblBalance.setText(MoneyFormatter.formatMasked());
            return;
        }

        if (MoneyVisibilityManager.getInstance().isVisible()) {
            String due = String.format("%.2f $ / %,.0f FC", currentSituation.totalDueUSD, currentSituation.totalDueCDF);
            String paid = String.format("%.2f $ / %,.0f FC", currentSituation.totalPaidUSD, currentSituation.totalPaidCDF);
            String bal = String.format("%.2f $ | %,.0f FC", currentSituation.balanceUSD, currentSituation.balanceCDF);

            lblTotalDue.setText(due);
            lblTotalPaid.setText(paid);
            lblBalance.setText(bal);
        } else {
            lblTotalDue.setText(MoneyFormatter.formatMasked());
            lblTotalPaid.setText(MoneyFormatter.formatMasked());
            lblBalance.setText(MoneyFormatter.formatMasked());
        }
    }

    @Override
    public void onShow() {
        refreshFinancialDisplay();
        filterHistory();
    }

    @Override
    public void onHide() {
        refreshFinancialDisplay();
        filterHistory();
    }
}
