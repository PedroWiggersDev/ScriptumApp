package com.udesc.scriptumapp.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.udesc.scriptumapp.R;
import com.udesc.scriptumapp.model.Category;
import com.udesc.scriptumapp.model.Document;
import com.udesc.scriptumapp.worker.DocumentProcessingWorker;

public class ScreenFragment extends Fragment {
    public static final String LOGIN = "login";
    public static final String CATEGORIES = "categories";
    public static final String DOCUMENTS = "documents";
    public static final String DETAIL = "detail";
    public static final String CREATE_DOCUMENT = "create_document";
    public static final String PROFILE = "profile";
    private static final String PAGE_KEY = "page";
    private static final String CATEGORY_ID_KEY = "category_id";
    private static final String CATEGORY_TITLE_KEY = "category_title";
    private static final String DOCUMENT_TITLE_KEY = "document_title";
    private static final String DOCUMENT_SUMMARY_KEY = "document_summary";
    private static final String DOCUMENT_STATUS_KEY = "document_status";
    private static final String DOCUMENT_CONTENT_KEY = "document_content";
    private ScriptumViewModel viewModel;
    private String page;
    private String categoryId;
    private String categoryTitle;
    private EditText documentTitleField;
    private EditText documentContentField;
    private TextView documentFormError;
    private TextView documentScanStatus;
    private View documentSelectedFile;
    private View documentScanActions;
    private RadioButton documentScanMode;
    private boolean scannedDocument = true;
    private boolean imageSelected;
    private Uri selectedImageUri;
    private File selectedImageFile;
    private UUID processingWorkId;
    private AlertDialog documentDialog;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String[]> imageLauncher;
    private File pendingCapture;

    public static ScreenFragment newInstance(String page, Bundle arguments) {
        ScreenFragment fragment = new ScreenFragment();
        Bundle bundle = arguments == null ? new Bundle() : new Bundle(arguments);
        bundle.putString(PAGE_KEY, page);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        page = arguments == null ? LOGIN : arguments.getString(PAGE_KEY, LOGIN);
        categoryId = arguments == null ? null : arguments.getString(CATEGORY_ID_KEY);
        categoryTitle = arguments == null ? null : arguments.getString(CATEGORY_TITLE_KEY);
        viewModel = new ViewModelProvider(requireActivity()).get(ScriptumViewModel.class);
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), this::handleCameraResult);
        imageLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleGalleryResult);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layout = layoutForPage();
        return inflater.inflate(layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        switch (page) {
            case LOGIN:
                bindLogin(view);
                break;
            case CATEGORIES:
                bindCategories(view);
                break;
            case DOCUMENTS:
                bindDocuments(view);
                break;
            case DETAIL:
                bindDocumentDetail(view);
                break;
            case CREATE_DOCUMENT:
                bindCreateDocument(view);
                break;
            case PROFILE:
                bindProfile(view);
                break;
            default:
                bindLogin(view);
                break;
        }
        if (getActivity() instanceof MainActivity && (CATEGORIES.equals(page) || PROFILE.equals(page))) {
            ((MainActivity) getActivity()).updateBottomNavigation(page);
        }
    }

    private int layoutForPage() {
        switch (page) {
            case CATEGORIES:
                return R.layout.fragment_categories;
            case DOCUMENTS:
                return R.layout.fragment_documents;
            case DETAIL:
                return R.layout.fragment_document_detail;
            case CREATE_DOCUMENT:
                return R.layout.dialog_create_document;
            case PROFILE:
                return R.layout.fragment_profile;
            case LOGIN:
            default:
                return R.layout.fragment_login;
        }
    }

    /** Valida os campos e persiste o token somente após a autenticação retornar sucesso. */
    private void bindLogin(View view) {
        EditText username = view.findViewById(R.id.login_username);
        EditText password = view.findViewById(R.id.login_password);
        TextView usernameError = view.findViewById(R.id.login_username_error);
        TextView passwordError = view.findViewById(R.id.login_password_error);
        TextView formError = view.findViewById(R.id.login_form_error);
        viewModel.getAuthenticationResult().observe(getViewLifecycleOwner(), event -> {
            OperationResult<String> result = event.getContentIfNotHandled();
            if (result == null || !isAdded()) {
                return;
            }
            setButtonEnabled(view, R.id.login_submit, true);
            if (result.error != null) {
                showError(formError, result.error.getMessage());
                return;
            }
            ((MainActivity) requireActivity()).showPage(CATEGORIES);
        });
        view.findViewById(R.id.login_submit).setOnClickListener(v -> {
            clearError(usernameError);
            clearError(passwordError);
            clearError(formError);
            String usernameValue = username.getText().toString().trim();
            String passwordValue = password.getText().toString();
            boolean valid = true;
            if (usernameValue.isEmpty()) {
                showError(usernameError, "Informe o usuário.");
                valid = false;
            }
            if (passwordValue.isEmpty()) {
                showError(passwordError, "Informe a senha.");
                valid = false;
            }
            if (!valid) {
                return;
            }
            setButtonEnabled(view, R.id.login_submit, false);
            viewModel.authenticate(usernameValue, passwordValue);
        });
    }

    /** Carrega categorias do serviço e cruza as recentes locais somente pelo ID da categoria. */
    private void bindCategories(View view) {
        RecyclerView categoriesList = view.findViewById(R.id.categories_list);
        RecyclerView recentList = view.findViewById(R.id.recent_categories_list);
        TextView categoriesEmpty = view.findViewById(R.id.categories_empty);
        TextView recentEmpty = view.findViewById(R.id.recent_categories_empty);
        categoriesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        viewModel.getCategoriesResult().observe(getViewLifecycleOwner(), event -> {
            OperationResult<ScriptumViewModel.CategoriesData> result = event.getContentIfNotHandled();
            if (result == null || !isAdded()) {
                return;
            }
            if (result.error != null) {
                categoriesEmpty.setText("Não foi possível carregar categorias: " + result.error.getMessage());
                return;
            }
            List<Category> categories = result.data.categories;
            categoriesList.setAdapter(new CategoryAdapter(categories, this::openCategory));
            categoriesEmpty.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
            if (categories.isEmpty()) {
                categoriesEmpty.setText("Nenhuma categoria disponível.");
            }
            List<Category> recent = result.data.recentCategories;
            recentList.setAdapter(new CategoryAdapter(recent, this::openCategory));
            recentEmpty.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
        });
        view.findViewById(R.id.category_create).setOnClickListener(v -> showCreateCategoryDialog(
                categoriesList, recentList, categoriesEmpty, recentEmpty));
        loadCategories(categoriesList, recentList, categoriesEmpty, recentEmpty);
    }

    /** Exibe o formulário de categoria em um diálogo e atualiza o catálogo após a resposta do serviço. */
    private void showCreateCategoryDialog(
            RecyclerView categoriesList,
            RecyclerView recentList,
            TextView categoriesEmpty,
            TextView recentEmpty) {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_category, null, false);
        EditText title = form.findViewById(R.id.category_title);
        TextView titleError = form.findViewById(R.id.category_title_error);
        TextView formError = form.findViewById(R.id.category_form_error);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(form)
                .create();
        viewModel.getCategoryCreationResult().observe(getViewLifecycleOwner(), event -> {
            OperationResult<Category> result = event.getContentIfNotHandled();
            if (result == null || !isAdded()) {
                return;
            }
            View submit = form.findViewById(R.id.category_create_submit);
            submit.setEnabled(true);
            if (result.error != null) {
                showError(formError, result.error.getMessage());
                return;
            }
            dialog.dismiss();
            loadCategories(categoriesList, recentList, categoriesEmpty, recentEmpty);
        });
        form.findViewById(R.id.category_create_cancel).setOnClickListener(v -> dialog.dismiss());
        form.findViewById(R.id.category_create_submit).setOnClickListener(v -> {
            clearError(titleError);
            clearError(formError);
            String value = title.getText().toString().trim();
            if (value.isEmpty()) {
                showError(titleError, "Informe o nome da categoria.");
                return;
            }
            View submit = form.findViewById(R.id.category_create_submit);
            submit.setEnabled(false);
            viewModel.createCategory(value);
        });
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setLayout(
                        Math.min(getResources().getDisplayMetrics().widthPixels - dp(40), dp(420)),
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });
        dialog.show();
    }

    private String fileToBase64(File file) {
        try (java.io.FileInputStream inputStream = new java.io.FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            inputStream.read(bytes);
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** Exibe o formulário de documento como diálogo com alternância entre digitação e escaneamento. */
    private void showCreateDocumentDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_document, null, false);
        documentDialog = new AlertDialog.Builder(requireContext())
                .setView(form)
                .create();
        documentDialog.setOnShowListener(ignored -> {
            if (documentDialog.getWindow() != null) {
                documentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                documentDialog.getWindow().setLayout(
                        Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(480)),
                        (int) (getResources().getDisplayMetrics().heightPixels * 0.82f));
            }
        });
        documentDialog.setOnCancelListener(ignored -> clearPendingDocument());
        bindDocumentForm(form, documentDialog);
        documentDialog.show();
    }

    private void loadCategories(
            RecyclerView categoriesList,
            RecyclerView recentList,
            TextView categoriesEmpty,
            TextView recentEmpty) {
        categoriesEmpty.setText("Carregando categorias...");
        categoriesEmpty.setVisibility(View.VISIBLE);
        viewModel.loadCategories();
    }

    /** Busca documentos remotos da categoria e mantém a busca local restrita à lista recebida. */
    private void bindDocuments(View view) {
        TextView categoryTitleView = view.findViewById(R.id.documents_category_title);
        TextView categoryCount = view.findViewById(R.id.documents_category_count);
        EditText search = view.findViewById(R.id.documents_search);
        TextView empty = view.findViewById(R.id.documents_empty);
        RecyclerView list = view.findViewById(R.id.documents_list);
        view.findViewById(R.id.documents_back).setOnClickListener(v -> ((MainActivity) requireActivity()).showPage(CATEGORIES));
        categoryTitleView.setText("Categoria: " + (categoryTitle == null ? "" : categoryTitle));
        categoryCount.setText("Carregando documentos...");
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        DocumentAdapter adapter = new DocumentAdapter(new ArrayList<>(), this::openDocument);
        list.setAdapter(adapter);
        viewModel.getDocumentsResult().observe(getViewLifecycleOwner(), event -> {
            OperationResult<List<Document>> result = event.getContentIfNotHandled();
            if (result == null || !isAdded()) {
                return;
            }
            if (result.error != null) {
                categoryCount.setText("Falha ao carregar documentos.");
                empty.setText("Não foi possível carregar documentos: " + result.error.getMessage());
                empty.setVisibility(View.VISIBLE);
                return;
            }
            List<Document> documents = result.data;
            categoryCount.setText(documents.size() + (documents.size() == 1 ? " documento" : " documentos"));
            adapter.setItems(documents);
            empty.setVisibility(documents.isEmpty() ? View.VISIBLE : View.GONE);
            if (documents.isEmpty()) {
                empty.setText("Esta categoria ainda não tem documentos.");
            }
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                adapter.filter(value.toString());
                empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                if (adapter.getItemCount() == 0) {
                    empty.setText("Nenhum documento encontrado.");
                }
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
        view.findViewById(R.id.document_create).setOnClickListener(v -> showCreateDocumentDialog());
        empty.setText("Carregando documentos...");
        viewModel.loadDocuments(categoryId);
    }

    /** Exibe os dados do documento recebido pela navegação, sem manter cópias em SQLite. */
    private void bindDocumentDetail(View view) {
        Bundle arguments = getArguments();
        view.<TextView>findViewById(R.id.detail_title).setText(arguments == null ? "" : arguments.getString(DOCUMENT_TITLE_KEY, ""));
        String status = arguments == null ? "" : arguments.getString(DOCUMENT_STATUS_KEY, "");
        String summary = arguments == null ? "" : arguments.getString(DOCUMENT_SUMMARY_KEY, "");
        view.<TextView>findViewById(R.id.detail_status).setText(status + (summary.isEmpty() ? "" : " • " + summary));
        String content = arguments == null ? "" : arguments.getString(DOCUMENT_CONTENT_KEY, "");
        view.<TextView>findViewById(R.id.detail_content).setText(content.isEmpty()
                ? "O conteúdo deste documento está vazio ou será disponibilizado após o processamento."
                : content);
        view.findViewById(R.id.detail_back).setOnClickListener(v -> ((MainActivity) requireActivity()).showPage(DOCUMENTS, categoryArguments()));
    }

    /** Configura entrada manual, captura, seleção de arquivo e envio do documento ao serviço. */
    private void bindCreateDocument(View view) {
        bindDocumentForm(view, null);
    }

    private void bindDocumentForm(View view, AlertDialog dialog) {
        documentTitleField = view.findViewById(R.id.document_title);
        documentContentField = view.findViewById(R.id.document_content);
        documentFormError = view.findViewById(R.id.document_form_error);
        documentScanStatus = view.findViewById(R.id.document_scan_status);
        documentSelectedFile = view.findViewById(R.id.document_selected_file);
        documentScanActions = view.findViewById(R.id.document_scan_actions);
        TextView titleError = view.findViewById(R.id.document_title_error);
        TextView contentError = view.findViewById(R.id.document_content_error);
        LinearLayout manualPanel = view.findViewById(R.id.document_manual_panel);
        LinearLayout scanPanel = view.findViewById(R.id.document_scan_panel);
        RadioGroup modeGroup = view.findViewById(R.id.document_mode_group);
        documentScanMode = view.findViewById(R.id.document_mode_scan);
        scannedDocument = true;
        imageSelected = false;
        selectedImageUri = null;
        selectedImageFile = null;
        processingWorkId = null;
        manualPanel.setVisibility(View.GONE);
        scanPanel.setVisibility(View.VISIBLE);
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            scannedDocument = checkedId == R.id.document_mode_scan;
            if (!scannedDocument && imageSelected) {
                removeSelectedImage();
            }
            manualPanel.setVisibility(scannedDocument ? View.GONE : View.VISIBLE);
            scanPanel.setVisibility(scannedDocument ? View.VISIBLE : View.GONE);
            clearError(contentError);
            clearError(documentFormError);
        });
        view.findViewById(R.id.document_take_photo).setOnClickListener(v -> launchCamera());
        view.findViewById(R.id.document_select_image).setOnClickListener(v -> imageLauncher.launch(new String[]{"image/*"}));
        view.findViewById(R.id.document_remove_file).setOnClickListener(v -> removeSelectedImage());
        viewModel.getTaskSubmissionResult().observe(getViewLifecycleOwner(), event -> {
            OperationResult<Boolean> result = event.getContentIfNotHandled();
            if (result == null || !isAdded()) {
                return;
            }
            setButtonEnabled(view, R.id.document_save, true);
            if (result.error != null || !Boolean.TRUE.equals(result.data)) {
                showError(documentFormError, result.error == null
                        ? "Não foi possível enviar o documento."
                        : result.error.getMessage());
                return;
            }
            if (dialog != null) {
                dialog.dismiss();
            }
            ((MainActivity) requireActivity()).showPage(DOCUMENTS, categoryArguments());
        });
        view.findViewById(R.id.document_save).setOnClickListener(v -> {
            clearError(titleError);
            clearError(contentError);
            clearError(documentFormError);
            String title = documentTitleField.getText().toString().trim();
            boolean valid = true;

            if (title.isEmpty()) {
                showError(titleError, "Informe o título do documento.");
                valid = false;
            }

            if (scannedDocument) {
                if (!imageSelected || selectedImageFile == null) {
                    showError(documentFormError, "Aguarde o processamento da imagem ou selecione uma foto.");
                    valid = false;
                }
            } else {
                String content = documentContentField.getText().toString().trim();
                if (content.isEmpty()) {
                    showError(contentError, "Digite o conteúdo do documento.");
                    valid = false;
                }
            }

            if (!valid) return;

            setButtonEnabled(view, R.id.document_save, false);

            if (scannedDocument) {

                String base64 = fileToBase64(selectedImageFile);
                if (base64 == null) {
                    showError(documentFormError, "Erro ao preparar a imagem.");
                    setButtonEnabled(view, R.id.document_save, true);
                    return;
                }
                viewModel.submitScannedTask(categoryId, title, base64);
            } else {
                String content = documentContentField.getText().toString().trim();
                viewModel.submitTask(categoryId, title, content);
            }
        });
        view.findViewById(R.id.document_cancel).setOnClickListener(v -> {
            if (dialog == null) {
                ((MainActivity) requireActivity()).showPage(DOCUMENTS, categoryArguments());
            } else {
                cancelDocumentDialog();
            }
        });
    }

    /** Mostra o nome da sessão local e remove o token SQLite ao sair da conta. */
    private void bindProfile(View view) {
        String username = viewModel.getUsername();
        view.<TextView>findViewById(R.id.profile_username).setText(username == null ? "—" : username);
        view.findViewById(R.id.profile_logout).setOnClickListener(v -> {
            viewModel.logout();
            ((MainActivity) requireActivity()).showPage(LOGIN);
        });
    }

    private void openCategory(Category category) {
        viewModel.recordViewedCategory(category);
        Bundle arguments = categoryArguments();
        arguments.putString(CATEGORY_ID_KEY, category.id);
        arguments.putString(CATEGORY_TITLE_KEY, category.name);
        ((MainActivity) requireActivity()).showPage(DOCUMENTS, arguments);
    }

    private void openDocument(Document document) {
        Bundle arguments = categoryArguments();
        arguments.putString(DOCUMENT_TITLE_KEY, document.title);
        arguments.putString(DOCUMENT_SUMMARY_KEY, document.getSummary());
        arguments.putString(DOCUMENT_STATUS_KEY, document.status + " • " + document.getStatusDetail());
        arguments.putString(DOCUMENT_CONTENT_KEY, document.getJoinedContent());
        ((MainActivity) requireActivity()).showPage(DETAIL, arguments);
    }

    private Bundle categoryArguments() {
        Bundle arguments = new Bundle();
        arguments.putString(CATEGORY_ID_KEY, categoryId);
        arguments.putString(CATEGORY_TITLE_KEY, categoryTitle);
        return arguments;
    }

    private void launchCamera() {
        if (imageSelected || pendingCapture != null) {
            showError(documentFormError, "Remova a imagem selecionada antes de escolher outra.");
            return;
        }
        File cameraDirectory = new File(requireContext().getCacheDir(), "scriptum-camera");
        if (!cameraDirectory.exists() && !cameraDirectory.mkdirs()) {
            showError(documentFormError, "Não foi possível preparar o armazenamento da captura.");
            return;
        }
        pendingCapture = new File(cameraDirectory, "capture_" + System.currentTimeMillis() + ".jpg");
        Uri destination = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                pendingCapture);
        cameraLauncher.launch(destination);
    }

    private void handleCameraResult(Boolean captured) {
        if (!Boolean.TRUE.equals(captured) || pendingCapture == null) {
            if (pendingCapture != null) {
                pendingCapture.delete();
                pendingCapture = null;
            }
            return;
        }
        File capturedImage = pendingCapture;
        pendingCapture = null;
        setSelectedImage("Foto_" + capturedImage.getName(), null, capturedImage);
    }

    private void handleGalleryResult(Uri uri) {
        if (uri == null) {
            return;
        }
        if (imageSelected) {
            showError(documentFormError, "Remova a imagem selecionada antes de escolher outra.");
            return;
        }
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException exception) {
            showError(documentFormError, "Não foi possível manter o acesso à imagem selecionada.");
            return;
        }
        setSelectedImage(getDisplayName(uri), uri, null);
    }

    private String getDisplayName(Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    return cursor.getString(column);
                }
            }
        }
        String path = uri.getLastPathSegment();
        return path == null ? "Imagem selecionada" : path;
    }

    private void setSelectedImage(String displayName, Uri uri, File file) {
        documentScanMode.setChecked(true);
        selectedImageUri = uri;
        selectedImageFile = file;
        imageSelected = true;
        documentSelectedFile.setVisibility(View.VISIBLE);
        documentScanActions.setVisibility(View.GONE);
        documentSelectedFile.<TextView>findViewById(R.id.document_selected_file_name).setText(displayName);
        documentScanStatus.setText("Imagem selecionada. O pré-processamento será executado em segundo plano.");
        clearError(documentFormError);
        if (uri != null) {
            enqueueProcessing(uri);
        } else {
            enqueueProcessing(file);
        }
    }

    private void removeSelectedImage() {
        if (processingWorkId != null) {
            WorkManager.getInstance(requireContext()).cancelWorkById(processingWorkId);
            processingWorkId = null;
        }
        if (selectedImageFile != null) {
            selectedImageFile.delete();
        }
        selectedImageUri = null;
        selectedImageFile = null;
        imageSelected = false;
        documentSelectedFile.setVisibility(View.GONE);
        documentScanActions.setVisibility(View.VISIBLE);
        documentScanStatus.setText("Imagem removida. Você poderá selecionar outra.");
        clearError(documentFormError);
    }

    private void cancelDocumentDialog() {
        clearPendingDocument();
        if (documentDialog != null) {
            documentDialog.dismiss();
        }
    }

    private void clearPendingDocument() {
        if (processingWorkId != null && isAdded()) {
            WorkManager.getInstance(requireContext()).cancelWorkById(processingWorkId);
            processingWorkId = null;
        }
        if (selectedImageFile != null) {
            selectedImageFile.delete();
        }
        if (pendingCapture != null) {
            pendingCapture.delete();
            pendingCapture = null;
        }
        selectedImageUri = null;
        selectedImageFile = null;
        imageSelected = false;
    }

    private void enqueueProcessing(File source) {
        enqueueProcessing(new Data.Builder().putString("source_path", source.getAbsolutePath()).build());
    }

    private void enqueueProcessing(Uri source) {
        enqueueProcessing(new Data.Builder().putString("source_uri", source.toString()).build());
    }

    private void enqueueProcessing(Data input) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DocumentProcessingWorker.class)
                .setInputData(input)
                .build();
        processingWorkId = request.getId();
        WorkManager.getInstance(requireContext()).enqueue(request);

        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(request.getId()).observe(getViewLifecycleOwner(), info -> {
            if (info == null || !isAdded()) {
                return;
            }
            if (info.getState() == WorkInfo.State.SUCCEEDED) {
                documentScanStatus.setText("Imagem pré-processada pronta para envio.");
                processingWorkId = null;

                String processedPath = info.getOutputData().getString("processed_path");
                if (processedPath != null) {
                    selectedImageFile = new File(processedPath);
                }

            } else if (info.getState() == WorkInfo.State.FAILED) {
                String error = info.getOutputData().getString("error");
                showError(documentFormError, error == null ? "Falha ao processar a imagem." : error);
            }
        });
    }

    private void showError(TextView label, String message) {
        label.setText(message);
        label.setVisibility(View.VISIBLE);
    }

    private void clearError(TextView label) {
        label.setText("");
        label.setVisibility(View.GONE);
    }

    private void setButtonEnabled(View root, int buttonId, boolean enabled) {
        root.findViewById(buttonId).setEnabled(enabled);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
