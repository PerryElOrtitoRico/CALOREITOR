package com.caloreitor.app.android;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import com.caloreitor.app.datos.CarpetaEventos;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Carpeta de eventos elegida con el selector de carpetas de Android (la que sincroniza Syncthing). Solo desde el hilo de E/S. */
public final class CarpetaSaf implements CarpetaEventos {
	private final ContentResolver resolver;
	private final Uri arbol;
	private final Uri raiz;
	// nombre -> documento, de la última vez que se listó
	private final Map<String, Uri> documentos = new HashMap<>();

	public CarpetaSaf(Context contexto, Uri arbol) {
		this.resolver = contexto.getContentResolver();
		this.arbol = arbol;
		this.raiz = DocumentsContract.buildDocumentUriUsingTree(arbol, DocumentsContract.getTreeDocumentId(arbol));
	}

	@Override
	public List<String> archivos() {
		documentos.clear();
		List<String> r = new ArrayList<>();
		Uri hijos = DocumentsContract.buildChildDocumentsUriUsingTree(arbol, DocumentsContract.getTreeDocumentId(arbol));
		try (Cursor c = resolver.query(hijos, new String[] {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
				DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
			if (c == null) throw new IllegalStateException("No se pudo listar la carpeta");
			while (c.moveToNext()) {
				String nombre = c.getString(1);
				if (nombre != null && nombre.endsWith(".jsonl") && (nombre.startsWith("catalogo-") || nombre.startsWith("dia-"))) {
					documentos.put(nombre, DocumentsContract.buildDocumentUriUsingTree(arbol, c.getString(0)));
					r.add(nombre);
				}
			}
		}
		return r;
	}

	@Override
	public List<String> leerLineas(String archivo) {
		Uri doc = documentos.get(archivo);
		if (doc == null) return List.of();
		try (InputStream in = resolver.openInputStream(doc)) {
			if (in == null) return List.of();
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			for (int n; (n = in.read(buf)) > 0; ) bytes.write(buf, 0, n);
			// Un archivo ajeno cortado a mitad de un carácter no rompe la lectura: esa línea falla al parsear y se ignora.
			List<String> r = new ArrayList<>();
			for (String l : new String(bytes.toByteArray(), StandardCharsets.UTF_8).split("\\r?\\n")) if (!l.isBlank()) r.add(l);
			return r;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void agregarLinea(String archivo, String linea) {
		try {
			Uri doc = documentos.get(archivo);
			if (doc == null) {
				archivos(); // por si lo creó otro (o la sincronización) desde la última vez
				doc = documentos.get(archivo);
			}
			if (doc == null) {
				doc = DocumentsContract.createDocument(resolver, raiz, "application/octet-stream", archivo);
				if (doc == null) throw new IllegalStateException("No se pudo crear " + archivo);
				documentos.put(archivo, doc);
			}
			try (OutputStream out = resolver.openOutputStream(doc, "wa")) {
				if (out == null) throw new FileNotFoundException(archivo);
				out.write((linea + "\n").getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void borrar(String archivo) {
		Uri doc = documentos.remove(archivo);
		if (doc == null) return;
		try {
			DocumentsContract.deleteDocument(resolver, doc);
		} catch (FileNotFoundException e) {
			// ya no estaba
		}
	}
}
