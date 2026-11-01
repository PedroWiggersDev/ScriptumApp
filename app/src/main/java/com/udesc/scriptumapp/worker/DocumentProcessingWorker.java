package com.udesc.scriptumapp.worker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DocumentProcessingWorker extends Worker {
    public DocumentProcessingWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        String sourcePath = getInputData().getString("source_path");
        String sourceUri = getInputData().getString("source_uri");
        if (sourcePath == null && sourceUri == null) {
            return failure("Caminho da imagem não informado.");
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try {
            decode(sourcePath, sourceUri, bounds);
        } catch (IOException exception) {
            return failure("Não foi possível abrir a imagem selecionada.");
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return failure("O arquivo selecionado não contém uma imagem válida.");
        }
        int sampleSize = 1;
        while (Math.max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > 3200) {
            sampleSize *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        Bitmap source;
        try {
            source = decode(sourcePath, sourceUri, options);
        } catch (IOException exception) {
            return failure("Não foi possível abrir a imagem selecionada.");
        }
        if (source == null) {
            return failure("Não foi possível decodificar a imagem.");
        }
        int maxDimension = Math.max(source.getWidth(), source.getHeight());
        float scale = Math.min(1f, 1600f / maxDimension);
        int width = Math.max(1, Math.round(source.getWidth() * scale));
        int height = Math.max(1, Math.round(source.getHeight() * scale));
        Bitmap processed = Bitmap.createScaledBitmap(source, width, height, true);
        File output = new File(getApplicationContext().getCacheDir(), "scriptum_scan_" + getId() + ".jpg");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            if (!processed.compress(Bitmap.CompressFormat.JPEG, 85, stream)) {
                return failure("Não foi possível salvar a imagem pré-processada.");
            }
        } catch (IOException exception) {
            return failure("Falha ao gravar a imagem pré-processada.");
        } finally {
            source.recycle();
            if (processed != source) {
                processed.recycle();
            }
        }
        // PLACEHOLDER: integrar OCR e envio do documento pela API quando os endpoints estiverem disponíveis.
        return Result.success(new androidx.work.Data.Builder().putString("processed_path", output.getAbsolutePath()).build());
    }

    private Bitmap decode(String path, String uri, BitmapFactory.Options options) throws IOException {
        if (path != null) {
            return BitmapFactory.decodeFile(path, options);
        }
        try (InputStream input = getApplicationContext().getContentResolver().openInputStream(Uri.parse(uri))) {
            return input == null ? null : BitmapFactory.decodeStream(input, null, options);
        }
    }

    private Result failure(String message) {
        return Result.failure(new androidx.work.Data.Builder().putString("error", message).build());
    }
}
