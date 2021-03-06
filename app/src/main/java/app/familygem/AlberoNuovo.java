package app.familygem;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.CharacterSet;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomVersion;
import org.folg.gedcom.model.Generator;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;

public class AlberoNuovo extends AppCompatActivity {

	View rotella;

	@Override
	protected void onCreate( Bundle bandolo ) {
		super.onCreate( bandolo );
		setContentView(R.layout.albero_nuovo);
		rotella = findViewById( R.id.nuovo_circolo );
		String referrer = Globale.preferenze.referrer; // Dataid proveniente da una condivisione
		boolean esisteDataId = referrer != null && referrer.matches("[0-9]{14}");

		// Scarica l'albero condiviso
		Button scaricaCondiviso = findViewById( R.id.bottone_scarica_condiviso );
		if( esisteDataId )
			// Non ha bisogno di permessi perché scarica e decomprime solo nello storage esterno dell'app
			scaricaCondiviso.setOnClickListener( v -> Facciata.scaricaCondiviso(this, referrer, rotella) );
		else
			scaricaCondiviso.setVisibility( View.GONE );

		// Crea un albero vuoto
		Button alberoVuoto = findViewById( R.id.bottone_albero_vuoto );
		if( esisteDataId ) {
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
				alberoVuoto.setBackgroundTintList( ColorStateList.valueOf(getResources().getColor(R.color.primarioChiaro)) );
		}
		alberoVuoto.setOnClickListener( v -> {
			View vistaMessaggio = LayoutInflater.from(this).inflate(R.layout.albero_nomina, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView( vistaMessaggio ).setTitle( R.string.title );
			TextView vistaTesto = vistaMessaggio.findViewById( R.id.nuovo_nome_testo );
			vistaTesto.setText( R.string.modify_later );
			vistaTesto.setVisibility( View.VISIBLE );
			EditText nuovoNome = vistaMessaggio.findViewById( R.id.nuovo_nome_albero );
			builder.setPositiveButton( R.string.create, (dialog, id) -> salva(nuovoNome.getText().toString()) )
					.setNeutralButton( R.string.cancel, null ).create().show();
			nuovoNome.setOnEditorActionListener( (view, action, event) -> {
				if( action == EditorInfo.IME_ACTION_DONE ) {
					salva( nuovoNome.getText().toString() );
					return true; // completa le azioni di salva()
				}
				return false; // Eventuali altri action che non esistono
			});
			vistaMessaggio.post( () -> {
				nuovoNome.requestFocus();
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.showSoftInput(nuovoNome, InputMethodManager.SHOW_IMPLICIT);
			});
		});

		Button scaricaEsempio = findViewById(R.id.bottone_scarica_esempio);
		// Non ha bisogno di permessi
		scaricaEsempio.setOnClickListener( v -> scaricaEsempio() );

		Button importaGedcom = findViewById(R.id.bottone_importa_gedcom);
		importaGedcom.setOnClickListener( v -> {
			int perm = ContextCompat.checkSelfPermission(v.getContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if( perm == PackageManager.PERMISSION_DENIED )
				ActivityCompat.requestPermissions( (AppCompatActivity)v.getContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1390 );
			else if( perm == PackageManager.PERMISSION_GRANTED )
				importaGedcom();
		});

		Button recuperaBackup = findViewById(R.id.bottone_recupera_backup);
		recuperaBackup.setOnClickListener( v -> {
			Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
			intent.setType( "application/zip" );
			startActivityForResult( intent, 219 );
		});
	}

	// Elabora la risposta alle richieste di permesso
	@Override
	public void onRequestPermissionsResult( int codice, String[] permessi, int[] accordi ) { // If request is cancelled, the result arrays are empty
		if( accordi.length > 0 && accordi[0] == PackageManager.PERMISSION_GRANTED ) {
			if( codice == 1390 ) {
				importaGedcom();
			}
		}
	}

	// Salva un nuovo albero
	void salva( String titolo ) {
		int num = Globale.preferenze.max() + 1;
		File fileJson = new File( getFilesDir(), num + ".json" );
		Globale.gc = new Gedcom();
		Globale.gc.setHeader( creaTestata( fileJson.getName() ) );
		Globale.gc.createIndexes();
		JsonParser jp = new JsonParser();
		try {
			FileUtils.writeStringToFile( fileJson, jp.toJson(Globale.gc), "UTF-8" );
		} catch (Exception e) {
			Toast.makeText( this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
			return;
		}
		Globale.preferenze.aggiungi( new Armadio.Cassetto(num, titolo, null, 0, 0, null, null, 0, null) );
		Globale.preferenze.salva();
		onBackPressed();
		Toast.makeText( this, R.string.tree_created, Toast.LENGTH_SHORT ).show();
	}

	// Scarica da Google Drive il file zip dei Simpson nella cache esterna dell'app, quindi senza bisogno di permessi
	void scaricaEsempio() {
		rotella.setVisibility( View.VISIBLE );
		DownloadManager gestoreScarico = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		// Evita download multipli
		Cursor curso = gestoreScarico.query( new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING) );
		if( curso.moveToFirst() ) {
			curso.close();
			findViewById(R.id.bottone_scarica_esempio).setEnabled(false);
			return;
		}
		String url = "https://drive.google.com/uc?export=download&id=1FT-60avkxrHv6G62pxXs9S6Liv5WkkKf";
		String percorsoZip = getExternalCacheDir() + "/the_Simpsons.zip";
		DownloadManager.Request richiesta = new DownloadManager.Request( Uri.parse( url ) )
				.setTitle( getString(R.string.simpsons_tree) )
				.setDescription( getString(R.string.family_gem_example) )
				.setMimeType( "application/zip" )
				.setNotificationVisibility( DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
				.setDestinationUri( Uri.parse( "file://" + percorsoZip ) );
		gestoreScarico.enqueue( richiesta );
		BroadcastReceiver alCompletamento = new BroadcastReceiver() {
			@Override
			public void onReceive( Context contesto, Intent intento ) {
				decomprimiZip( contesto, percorsoZip, null );
				unregisterReceiver( this );
			}
		};
		registerReceiver( alCompletamento, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE) );
		// ACTION_DOWNLOAD_COMPLETE intende il completamento di QUALSIASI download che è in corso, non solo questo.
	}

	// Decomprime il file zip nella memoria esterna e cartella col numero dell'albero
	// Usato ugualmente da: esempio Simpson, file di backup e alberi condivisi
	static void decomprimiZip( final Context contesto, String percorsoZip, Uri uriZip ) {
		int numAlbero = Globale.preferenze.max() + 1;
		File dirImmagini = contesto.getExternalFilesDir( String.valueOf(numAlbero) );
		if( !dirImmagini.exists() )
			dirImmagini.mkdir();
		try {
			InputStream is;
			if( percorsoZip != null )
				is = new FileInputStream( percorsoZip );
			// toDO da android 10 necessario rivedere il metodo di accesso a file nelle cartelle esterne
				// temporaneamente risolto nel manifest con 'requestLegacyExternalStorage'
			else
				is = contesto.getContentResolver().openInputStream( uriZip );
			ZipInputStream zis = new ZipInputStream( is );
			ZipEntry zipEntry;
			int len;
			byte[] buffer = new byte[1024];
			while( (zipEntry = zis.getNextEntry()) != null ) {
				String percorsoFile;
				if( zipEntry.getName().equals("tree.json") )
					percorsoFile = contesto.getFilesDir() +"/"+ numAlbero + ".json";
				else if( zipEntry.getName().equals("settings.json") )
					percorsoFile = contesto.getCacheDir() +"/settings.json";
				else // è un file nella cartella 'media'
					percorsoFile = dirImmagini +"/"+ zipEntry.getName().replace( "media/", "" );
				File newFile = new File( percorsoFile );
				FileOutputStream fos = new FileOutputStream( newFile );
				while( ( len = zis.read( buffer ) ) > 0 ) {
					fos.write( buffer, 0, len );
				}
				fos.close();
			}
			zis.closeEntry();
			zis.close();
			// Legge le impostazioni e le salva nelle preferenze
			File fileImpostazioni = new File( contesto.getCacheDir(),"settings.json" );
			String json = FileUtils.readFileToString( fileImpostazioni, "UTF-8" );
			Gson gson = new Gson();
			Armadio.CassettoCondiviso cassa = gson.fromJson( json, Armadio.CassettoCondiviso.class );
			Armadio.Cassetto cassetto = new Armadio.Cassetto( numAlbero, cassa.titolo, dirImmagini.getPath(),
					cassa.individui, cassa.generazioni, cassa.radice, cassa.condivisioni, cassa.grado, null );
			Globale.preferenze.aggiungi( cassetto );
			fileImpostazioni.delete();
			//fileZip.delete();
			// Albero proveniente da condivisione destinato al confronto
			if( cassa.grado == 9 && confronta(contesto,cassetto) ) {
				cassetto.grado = 20; // lo marchia come derivato
			} // Albero condiviso scaricato automaticamente dopo la prima installazione
			else if( Globale.preferenze.referrer != null ) {
				Globale.preferenze.referrer = null;
				if( contesto instanceof Alberi ) { // Download dal dialogo in Alberi
					Alberi alberi = (Alberi) contesto;
					alberi.runOnUiThread( () -> {
						alberi.rotella.setVisibility( View.GONE );
						alberi.aggiornaLista();
					});
				} else // Downolad da AlberoNuovo
					contesto.startActivity( new Intent( contesto, Alberi.class ) );
			} else { // Albero di esempio, di backup, o di condivisione
				contesto.startActivity( new Intent( contesto, Alberi.class ) );
			}
			Globale.preferenze.salva();
			U.tosta( (Activity)contesto, R.string.tree_imported_ok );
		} catch( Exception e ) {
			U.tosta( (Activity)contesto, e.getLocalizedMessage() );
		}
	}

	// Fa scegliere un file Gedcom da importare
	void importaGedcom() {
		Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
		// KitKat disabilita i file .ged nella cartella Download se il type è 'application/*'
		if( Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT )
			intent.setType( "*/*" );
		else
			intent.setType( "application/*" );
		startActivityForResult( intent,630 );
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, final Intent data ) {
		super.onActivityResult( requestCode, resultCode, data );
		// Importa un file Gedcom scelto con SAF
		if( resultCode == RESULT_OK && requestCode == 630 ){
			try {
				// Legge l'input
				Uri uri = data.getData();
				InputStream input = getContentResolver().openInputStream(uri);
				Gedcom gc = new ModelParser().parseGedcom( input );
				if( gc.getHeader() == null ) {
					Toast.makeText( this, R.string.invalid_gedcom, Toast.LENGTH_LONG ).show();
					return;
				}
				gc.createIndexes(); // necessario per poi calcolare le generazioni
				// Salva il file Json
				int nuovoNum = Globale.preferenze.max() + 1;
				PrintWriter pw = new PrintWriter( getFilesDir() + "/" + nuovoNum + ".json" );
				JsonParser jp = new JsonParser();
				pw.print( jp.toJson(gc) );
				pw.close();
				// Nome albero e percorso della cartella
				String percorso = F.uriPercorsoFile( uri );
				String nomeAlbero;
				String percorsoCartella = null;
				if( percorso != null && percorso.lastIndexOf('/') > 0 ) { // è un percorso completo del file gedcom
					File fileGedcom = new File( percorso );
					percorsoCartella = fileGedcom.getParent();
					nomeAlbero = fileGedcom.getName();
				} else if( percorso != null ) { // È solo il nome del file 'famiglia.ged'
					nomeAlbero = percorso;
				} else // percorso null
					nomeAlbero = getString( R.string.tree ) + " " + nuovoNum;
				if( nomeAlbero.lastIndexOf('.') > 0 ) // Toglie l'estensione
					nomeAlbero = nomeAlbero.substring(0, nomeAlbero.lastIndexOf('.'));
				// Salva le impostazioni in preferenze
				String idRadice = U.trovaRadice(gc);
				Globale.preferenze.aggiungi( new Armadio.Cassetto( nuovoNum, nomeAlbero, percorsoCartella,
						gc.getPeople().size(), InfoAlbero.quanteGenerazioni(gc,idRadice), idRadice, null, 0, null ) );
				Globale.preferenze.salva();
				// Se necessario propone di mostrare le funzioni avanzate
				if( !gc.getSources().isEmpty() && !Globale.preferenze.esperto ) {
					new AlertDialog.Builder(this).setMessage( R.string.complex_tree_advanced_tools )
							.setPositiveButton( android.R.string.ok, (dialog, i) -> {
								Globale.preferenze.esperto = true;
								Globale.preferenze.salva();
								concludiImportaGedcom();
							}).setNegativeButton( android.R.string.cancel, (dialog, i) -> concludiImportaGedcom() )
							.show();
				} else
					concludiImportaGedcom();
			} catch( Exception e ) {
				Toast.makeText( this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
			}
		}

		// Estrae un file ZIP di backup, mantenendo la compatibilità con il precedente tipo di backup:
		// - un backup di tipo vecchio SOSTITUISCE tutti gli alberi esistenti
		// - un backup nuovo AGGIUNGE un singolo albero
		if( resultCode == RESULT_OK && requestCode == 219 ){
			try {
				boolean ceFileSettings = false;
				boolean ceFilePreferenze = false;
				final ZipInputStream zis = new ZipInputStream( getContentResolver().openInputStream( data.getData() ) );
				ZipEntry zipEntry;
				while( (zipEntry = zis.getNextEntry()) != null ) {
					if(zipEntry.getName().equals( "settings.json" ))
						ceFileSettings = true;
					if(zipEntry.getName().equals( "preferenze.json" ))
						ceFilePreferenze = true;
				}
				zis.closeEntry();
				zis.close();
				if( ceFileSettings ) {
					decomprimiZip( this, null, data.getData() );
				} else if(ceFilePreferenze){
					AlertDialog.Builder builder = new AlertDialog.Builder( AlberoNuovo.this );
					builder.setTitle( R.string.warning );
					builder.setMessage( R.string.old_backup_overwrite );
					builder.setPositiveButton( android.R.string.yes, (dialog, id) -> {
						try {
							ZipInputStream zis2 = new ZipInputStream( getContentResolver().openInputStream( data.getData() ) );
							ZipEntry zipEntry2;
							int len;
							byte[] buffer = new byte[1024];
							while( (zipEntry2 = zis2.getNextEntry()) != null ) {
								File newFile = new File( getFilesDir(), zipEntry2.getName() );
								FileOutputStream fos = new FileOutputStream(newFile);
								while ((len = zis2.read(buffer)) > 0) {
									fos.write(buffer, 0, len);
								}
								fos.close();
							}
							zis2.closeEntry();
							zis2.close();
							Globale.avvia( getApplicationContext() );
							startActivity( new Intent( AlberoNuovo.this, Alberi.class ));
						} catch( IOException e ) {
							e.printStackTrace();
						}
					}).setNeutralButton( android.R.string.cancel, null ).create().show();
				} else
					Toast.makeText( AlberoNuovo.this, R.string.backup_invalid, Toast.LENGTH_LONG ).show();
			} catch( Exception e ) {
				Toast.makeText( AlberoNuovo.this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
			}
		}
	}

	void concludiImportaGedcom() {
		onBackPressed();
		Toast.makeText( this, R.string.tree_imported_ok, Toast.LENGTH_SHORT ).show();
	}

	// Confronta le date di invio degli alberi esistenti
	// Se trova almeno un albero originario tra quelli esistenti apre il confronto e restituisce true
	static boolean confronta( Context contesto, Armadio.Cassetto albero2 ) {
		if( albero2.condivisioni != null )
			for( Armadio.Cassetto alb : Globale.preferenze.alberi )
				if( alb.id != albero2.id && alb.condivisioni != null && alb.grado != 20  && alb.grado != 30 )
					for( int i = alb.condivisioni.size()-1; i >= 0; i-- ) { // Le condivisioni dall'ultima alla prima
						Armadio.Invio invio = alb.condivisioni.get(i);
						for( Armadio.Invio invio2 : albero2.condivisioni )
							if( invio.data != null && invio.data.equals( invio2.data ) ) {
								contesto.startActivity( new Intent( contesto, Compara.class )
									.putExtra( "idAlbero", alb.id )
									.putExtra( "idAlbero2", albero2.id )
									.putExtra( "idData", invio.data )
								);
								return true;
							}
					}
		return false;
	}

	// Crea l'intestazione standard per questa app
	public static Header creaTestata( String nomeFile ) {
		Header testa = new Header();
		Generator app = new Generator();
		app.setValue( "FAMILY_GEM" );
		app.setName( "Family Gem" );
		app.setVersion( BuildConfig.VERSION_NAME );
		testa.setGenerator( app );
		testa.setFile( nomeFile );
		GedcomVersion versione = new GedcomVersion();
		versione.setForm( "LINEAGE-LINKED" );
		versione.setVersion( "5.5.1" );
		testa.setGedcomVersion( versione );
		CharacterSet codifica = new CharacterSet();
		codifica.setValue( "UTF-8" );
		testa.setCharacterSet( codifica );
		Locale loc = new Locale( Locale.getDefault().getLanguage() );
		// C'è anche   Resources.getSystem().getConfiguration().locale.getLanguage() che ritorna lo stesso 'it'
		testa.setLanguage( loc.getDisplayLanguage( Locale.ENGLISH ) );	// ok prende la lingua di sistema in inglese, non nella lingua locale
		// in header ci sono due campi data: TRANSMISSION_DATE un po' forzatamente può contenere la data di ultima modifica
		testa.setDateTime( U.dataTempoAdesso() );
		return testa;
	}

	// Freccia indietro nella toolbar come quella hardware
	@Override
	public boolean onOptionsItemSelected( MenuItem i ) {
		onBackPressed();
		return true;
	}
}