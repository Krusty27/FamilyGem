// Attrezzi utili per tutto il programma

package app.familygem;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Change;
import org.folg.gedcom.model.DateTime;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.Submitter;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.ExtensionContainer;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.NoteContainer;
import org.folg.gedcom.model.NoteRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Source;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.SourceCitationContainer;
import org.folg.gedcom.parser.JsonParser;
import org.joda.time.Months;
import org.joda.time.Years;
import app.familygem.dettaglio.ArchivioRef;
import app.familygem.dettaglio.Autore;
import app.familygem.dettaglio.Cambiamenti;
import app.familygem.dettaglio.CitazioneFonte;
import app.familygem.dettaglio.Famiglia;
import app.familygem.dettaglio.Fonte;
import app.familygem.dettaglio.Immagine;
import app.familygem.dettaglio.Nota;
import app.familygem.visita.ListaMediaContenitore;
import app.familygem.visita.RiferimentiNota;
import app.familygem.visita.TrovaPila;

public class U {

	static String s( int id ) {
		return Globale.contesto.getString(id);
	}

	// Da usare dove capita che 'Globale.gc' possa essere null per ricaricarlo
	static void gedcomSicuro( Gedcom gc ) {
		if( gc == null )
			Globale.gc = Alberi.leggiJson( Globale.preferenze.idAprendo );
	}

	// restituisce l'id della Person iniziale di un Gedcom
	static String trovaRadice( Gedcom gc ) {
		if( gc.getHeader() != null)
			if( valoreTag( gc.getHeader().getExtensions(), "_ROOT" ) != null )
				return valoreTag( gc.getHeader().getExtensions(), "_ROOT" );
		if( !gc.getPeople().isEmpty() )
				return gc.getPeople().get(0).getId();
		return null;
	}
	
	// riceve una Person e restituisce stringa con nome e cognome principale
	static String epiteto( Person p ) {
		if( p != null && !p.getNames().isEmpty() )
			return nomeCognome( p.getNames().get(0) );
		return "[" + s(R.string.no_name) + "]";
	}

	// riceve una Person e restituisce il titolo nobiliare
	static String titolo( Person p ) {
		// GEDCOM standard INDI.TITL
		for( EventFact ef : p.getEventsFacts() )
			if( ef.getTag() != null && ef.getTag().equals("TITL") && ef.getValue() != null )
				return ef.getValue();
		// Così invece prende INDI.NAME._TYPE.TITL, vecchio metodo di org.folg.gedcom
		for( Name n : p.getNames() )
			if( n.getType() != null && n.getType().equals("TITL") && n.getValue() != null )
				return n.getValue();
		return "";
	}

	static String[] NAME_TYPES = { "aka", "birth", "immigrant", "maiden", "married" };
	static String[] TIPI_NOME = { s(R.string.aka), s(R.string.birth), s(R.string.immigrant), s(R.string.maiden), s(R.string.married) };

	// Riceve un NAME_TYPE e restituisce la corrispondente traduzione
	static String tipoNomeTradotto(String type) {
		int index = Arrays.asList(NAME_TYPES).indexOf(type);
		return index >= 0 ? TIPI_NOME[index] : type;
	}

	// Restituisce il nome e cognome addobbato di un Name
	static String nomeCognome( Name n ) {
		String completo = "";
		if( n.getValue() != null ) {
			String grezzo = n.getValue().trim();
			if( grezzo.indexOf('/') > -1 ) // Se c'è un cognome tra '/'
				completo = grezzo.substring( 0, grezzo.indexOf('/') ).trim(); // nome
			if (n.getNickname() != null)
				completo += " \"" + n.getNickname() + "\"";
			if( grezzo.indexOf('/') < grezzo.lastIndexOf('/') ) {
				completo += " " + grezzo.substring( grezzo.indexOf('/') + 1, grezzo.lastIndexOf('/') ).trim(); // cognome
			}
			if( grezzo.length() - 1 > grezzo.lastIndexOf('/') )
				completo += " " + grezzo.substring( grezzo.lastIndexOf('/') + 1 ).trim(); // dopo il cognome
		} else {
			if( n.getPrefix() != null )
				completo = n.getPrefix();
			if( n.getGiven() != null )
				completo += " " + n.getGiven();
			if( n.getSurname() != null )
				completo += " " + n.getSurname();
			if( n.getSuffix() != null )
				completo += " " + n.getSuffix();
		}
		completo = completo.trim();
		return completo.isEmpty() ? "[" + s(R.string.empty_name) + "]" : completo;
	}

	// Restituisce il cognome di una persona
	static String cognome( Person p ) {
		String cognome = "";
		if( !p.getNames().isEmpty() ) {
			Name name = p.getNames().get(0);
			String grezzo = name.getValue();
			if( grezzo != null && grezzo.indexOf('/') < grezzo.lastIndexOf('/') )
				cognome = grezzo.substring( grezzo.indexOf('/') + 1, grezzo.lastIndexOf('/') ).trim();
			else if( name.getSurname() != null )
				cognome = name.getSurname();
		}
		return cognome;
	}

	// Riceve una Person e restituisce il sesso: 0 senza SEX, 1 Maschio, 2 Femmina, 3 Undefinito, 4 altro
	public static int sesso( Person p ) {
		for( EventFact fatto : p.getEventsFacts() ) {
			if( fatto.getTag()!=null && fatto.getTag().equals("SEX") ) {
				if( fatto.getValue() == null )
					return 4;  // c'è 'SEX' ma il valore è vuoto
				else {
					switch( fatto.getValue() ) {
						case "M": return 1;
						case "F": return 2;
						case "U": return 3;
						default: return 4; // altro valore
					}
				}
			}
		}
		return 0; // SEX non c'è
	}

	// Riceve una person e trova se è morto o seppellito
	static boolean morto( Person p ) {
		for( EventFact fatto : p.getEventsFacts() ) {
			if( fatto.getTag().equals( "DEAT" ) || fatto.getTag().equals( "BURI" ) )
				return true;
		}
		return false;
	}

	// Riceve una Person e restituisce una stringa con gli anni di nascita e morte e l'età eventualmente
	static String dueAnni( Person p, boolean conEta ) {
		String anni = "";
		String annoFine = "";
		Datatore inizio = null, fine = null;
		for( EventFact unFatto : p.getEventsFacts() ) {
			if( unFatto.getTag() != null && unFatto.getTag().equals("BIRT") && unFatto.getDate() != null ) {
				inizio = new Datatore( unFatto.getDate() );
				anni = inizio.scriviAnno();
				break;
			}
		}
		for( EventFact unFatto : p.getEventsFacts() ) {
			if( unFatto.getTag() != null && unFatto.getTag().equals("DEAT") && unFatto.getDate() != null ) {
				fine = new Datatore( unFatto.getDate() );
				annoFine = fine.scriviAnno();
				if( !anni.isEmpty() && !annoFine.isEmpty() )
					anni += " – ";
				anni += annoFine;
				break;
			}
		}
		// Aggiunge l'età tra parentesi
		if( conEta && inizio != null && inizio.tipo <= 3 && !inizio.data1.format.toPattern().equals(Datatore.G_M) ) {
			LocalDate dataInizio = new LocalDate( inizio.data1.date ); // converte in joda time
			// Se è ancora vivo la fine è adesso
			if( fine == null && dataInizio.isBefore(LocalDate.now()) && Years.yearsBetween(dataInizio,LocalDate.now()).getYears() < 120 && !morto(p) ) {
				fine = new Datatore( String.format(Locale.ENGLISH,"%te %<Tb %<tY",new Date()) ); // un po' assurdo dover qui passare per Datatore...
				annoFine = fine.scriviAnno();
			}
			if( fine != null && fine.tipo <= 3 && !annoFine.equals("") ) { // date plausibili
				LocalDate dataFine = new LocalDate( fine.data1.date );
				String misura = "";
				int eta = Years.yearsBetween( dataInizio, dataFine ).getYears();
				if( eta < 2 ) {
					eta = Months.monthsBetween( dataInizio, dataFine ).getMonths(); // todo e se nella data non c'è il mese / giorno?
					misura = " " + Globale.contesto.getText( R.string.months );
					if( eta < 2 ) {
						eta = Days.daysBetween( dataInizio, dataFine ).getDays();
						misura = " " + Globale.contesto.getText( R.string.days );;
					}
				}
				if( eta >= 0 )
					anni += "  (" + eta + misura + ")";
			}
		}
		return anni;
	}

	// Estrae i soli numeri da una stringa che può contenere anche lettere
	static int soloNumeri( String id ) {
		//return Integer.parseInt( id.replaceAll("\\D+","") );	// sintetico ma lento
		int num = 0;
		int x = 1;
		for( int i = id.length()-1; i >= 0; --i ){
			int c = id.charAt( i );
			if( c > 47 && c < 58 ){
				num += (c-48) * x;
				x *= 10;
			}
		}
		return num;
	}

	// Genera il nuovo id seguente a quelli già esistenti
	static int max;
	public static String nuovoId( Gedcom gc, Class classe ) {
		max = 0;
		String pre = "";
		if( classe == Note.class ) {
			pre = "N";
			for( Note n : gc.getNotes() )
				calcolaMax( n );
		} else if( classe == Submitter.class ) {
			pre = "U";
			for( Submitter a : gc.getSubmitters() )
				calcolaMax( a );
		} else if( classe == Repository.class ) {
			pre = "R";
			for( Repository r : gc.getRepositories() )
				calcolaMax( r );
		} else if( classe == Media.class ) {
			pre = "M";
			for( Media m : gc.getMedia() )
				calcolaMax( m );
		} else if( classe == Source.class ) {
			pre = "S";
			for( Source f : gc.getSources() )
				calcolaMax( f );
		} else if( classe == Person.class ) {
			pre = "I";
			for( Person p : gc.getPeople() )
				calcolaMax( p );
		} else if( classe == Family.class ) {
			pre = "F";
			for( Family f : gc.getFamilies() )
				calcolaMax( f );
		}
		return pre + (max+1);
	}

	private static void calcolaMax( Object oggetto ) {
		try {
			String idStringa = (String) oggetto.getClass().getMethod("getId").invoke( oggetto );
			int num = soloNumeri( idStringa );
			if( num > max )	max = num;
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

	// Copia testo negli appunti
	static void copiaNegliAppunti(CharSequence label, CharSequence text) {
		ClipboardManager clipboard = (ClipboardManager) Globale.contesto.getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText( label, text );
		if (clipboard != null) clipboard.setPrimaryClip(clip);
	}

	// Restituisce la lista di estensioni
	@SuppressWarnings("unchecked")
	public static List<Estensione> trovaEstensioni( ExtensionContainer contenitore ) {
		if( contenitore.getExtension( "folg.more_tags" ) != null ) {
			List<Estensione> lista = new ArrayList<>();
			for( GedcomTag est : (List<GedcomTag>)contenitore.getExtension("folg.more_tags") ) {
				String testo = scavaEstensione(est,0);
				if( testo.endsWith("\n") )
					testo = testo.substring( 0, testo.length()-1 );
				lista.add( new Estensione( est.getTag(), testo, est ) );
			}
			return lista;
		}
		return Collections.emptyList();
	}

	// Costruisce un testo con il contenuto ricorsivo dell'estensione
	public static String scavaEstensione( GedcomTag pacco, int grado ) {
		String testo = "";
		if( grado > 0 )
			testo += pacco.getTag() +" ";
		if( pacco.getValue() != null )
			testo += pacco.getValue() +"\n";
		else if( pacco.getId() != null )
			testo += pacco.getId() +"\n";
		else if( pacco.getRef() != null )
			testo += pacco.getRef() +"\n";
		for( GedcomTag unPezzo : pacco.getChildren() )
			testo += scavaEstensione( unPezzo, ++grado );
		return testo;
	}

	public static void eliminaEstensione( GedcomTag estensione, Object contenitore, View vista ) {
		if( contenitore instanceof ExtensionContainer ) { // IndividuoEventi
			ExtensionContainer exc = (ExtensionContainer) contenitore;
			@SuppressWarnings("unchecked")
			List<GedcomTag> lista = (List<GedcomTag>) exc.getExtension( "folg.more_tags" );
			lista.remove( estensione );
			if( lista.isEmpty() )
				exc.getExtensions().remove( "folg.more_tags" );
			if( exc.getExtensions().isEmpty() )
				exc.setExtensions( null );
		} else if( contenitore instanceof GedcomTag ) { // Dettaglio
			GedcomTag gt = (GedcomTag) contenitore;
			gt.getChildren().remove( estensione );
			if( gt.getChildren().isEmpty() )
				gt.setChildren( null );
		}
		Memoria.annullaIstanze(estensione);
		if( vista != null )
			vista.setVisibility( View.GONE );
	}

	// Restituisce il valore di un determinato tag in una estensione (GedcomTag)
	@SuppressWarnings("unchecked")
	static String valoreTag( Map<String,Object> mappaEstensioni, String nomeTag ) {
		for( Map.Entry<String,Object> estensione : mappaEstensioni.entrySet() ) {
			List<GedcomTag> listaTag = (ArrayList<GedcomTag>) estensione.getValue();
			for( GedcomTag unPezzo : listaTag ) {
				//l( unPezzo.getTag() +" "+ unPezzo.getValue() );
				if( unPezzo.getTag().equals( nomeTag ) ) {
					if( unPezzo.getId() != null )
						return unPezzo.getId();
					else if( unPezzo.getRef() != null )
						return unPezzo.getRef();
					else
						return unPezzo.getValue();
				}
			}
		}
		return null;
	}

	// Metodi di creazione di elementi di lista

	// aggiunge a un Layout una generica voce titolo-testo
	// Usato seriamente solo da dettaglio.Cambiamenti
	public static void metti( LinearLayout scatola, String tit, String testo ) {
		View vistaPezzo = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_fatto, scatola, false );
		scatola.addView( vistaPezzo );
		((TextView)vistaPezzo.findViewById( R.id.fatto_titolo )).setText( tit );
		TextView vistaTesto = vistaPezzo.findViewById( R.id.fatto_testo );
		if( testo == null ) vistaTesto.setVisibility( View.GONE );
		else {
			vistaTesto.setText( testo );
			//((TextView)vistaPezzo.findViewById( R.id.fatto_edita )).setText( testo );
		}
		//((Activity)scatola.getContext()).registerForContextMenu( vistaPezzo );
	}

	// Compone il testo coi dettagli di un individuo e lo mette nella vista testo
	// inoltre restituisce lo stesso testo per Confrontatore
	static String dettagli( Person tizio, TextView vistaDettagli ) {
		String anni = dueAnni( tizio, true );
		String luoghi = Anagrafe.dueLuoghi( tizio );
		if( anni.isEmpty() && luoghi.isEmpty() && vistaDettagli != null ) {
			vistaDettagli.setVisibility( View.GONE );
		} else {
			if( ( anni.length() > 10 || luoghi.length() > 20 ) && ( !anni.isEmpty() && !luoghi.isEmpty() ) )
				anni += "\n" + luoghi;
			else
				anni += "   " + luoghi;
			if( vistaDettagli != null ) {
				vistaDettagli.setText( anni.trim() );
				vistaDettagli.setVisibility( View.VISIBLE );
			}
		}
		return anni.trim();
	}

	public static View mettiIndividuo( LinearLayout scatola, Person persona, String ruolo ) {
		View vistaIndi = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_individuo, scatola, false);
		scatola.addView( vistaIndi );
		TextView vistaRuolo = vistaIndi.findViewById( R.id.indi_ruolo );
		if( ruolo == null ) vistaRuolo.setVisibility( View.GONE );
		else vistaRuolo.setText( ruolo );
		TextView vistaNome = vistaIndi.findViewById( R.id.indi_nome );
		String nome = epiteto(persona);
		if( nome.isEmpty() && ruolo != null ) vistaNome.setVisibility( View.GONE );
		else vistaNome.setText( nome );
		TextView vistaTitolo = vistaIndi.findViewById(R.id.indi_titolo);
		String titolo = titolo( persona );
		if( titolo.isEmpty() ) vistaTitolo.setVisibility( View.GONE );
		else vistaTitolo.setText( titolo );
		dettagli( persona, vistaIndi.findViewById( R.id.indi_dettagli ) );
		F.unaFoto( Globale.gc, persona, vistaIndi.findViewById(R.id.indi_foto) );
		if( !morto(persona) )
			vistaIndi.findViewById( R.id.indi_lutto ).setVisibility( View.GONE );
		if( sesso(persona) == 1 )
			vistaIndi.findViewById(R.id.indi_bordo).setBackgroundResource( R.drawable.casella_bordo_maschio );
		if( sesso(persona) == 2 )
			vistaIndi.findViewById(R.id.indi_bordo).setBackgroundResource( R.drawable.casella_bordo_femmina );
		vistaIndi.setTag( persona.getId() );
		return vistaIndi;
	}

	// Tutte le note di un oggetto
	public static void mettiNote( LinearLayout scatola, Object contenitore, boolean dettagli ) {
		for( final Note nota : ((NoteContainer)contenitore).getAllNotes( Globale.gc ) ) {
			mettiNota( scatola, nota, dettagli );
		}
	}

	// Aggiunge una singola nota a un layout, con i dettagli o no
	static void mettiNota( final LinearLayout scatola, final Note nota, boolean dettagli ) {
		final Context contesto = scatola.getContext();
		View vistaNota = LayoutInflater.from(contesto).inflate( R.layout.pezzo_nota, scatola, false);
		scatola.addView( vistaNota );
		TextView testoNota = vistaNota.findViewById( R.id.nota_testo );
		testoNota.setText( nota.getValue() );
		int quanteCitaFonti = nota.getSourceCitations().size();
		TextView vistaCitaFonti = vistaNota.findViewById( R.id.nota_fonti );
		if( quanteCitaFonti > 0 && dettagli ) vistaCitaFonti.setText( String.valueOf(quanteCitaFonti) );
		else vistaCitaFonti.setVisibility( View.GONE );
		testoNota.setEllipsize( TextUtils.TruncateAt.END );
		if( dettagli ) {
			testoNota.setMaxLines( 10 );
			vistaNota.setTag( R.id.tag_oggetto, nota );
			if( contesto instanceof Individuo ) { // Fragment individuoEventi
				((AppCompatActivity)contesto).getSupportFragmentManager()
						.findFragmentByTag( "android:switcher:" + R.id.schede_persona + ":1" )	// non garantito in futuro
						.registerForContextMenu( vistaNota );
			} else if( scatola.getId() != R.id.dispensa_scatola ) // nelle AppCompatActivity tranne che nella dispensa
				((AppCompatActivity)contesto).registerForContextMenu( vistaNota );
			vistaNota.setOnClickListener( v -> {
				if( nota.getId() != null )
					Memoria.setPrimo( nota );
				else
					Memoria.aggiungi( nota );
				contesto.startActivity( new Intent( contesto, Nota.class ) );
			});
		} else {
			testoNota.setMaxLines( 3 );
		}
	}

	static void scollegaNota( Note nota, Object contenitore, View vista ) {
		List<NoteRef> lista = ((NoteContainer)contenitore).getNoteRefs();
		for( NoteRef ref : lista )
			if( ref.getNote(Globale.gc).equals( nota ) ) {
				lista.remove( ref );
				break;
			}
		((NoteContainer)contenitore).setNoteRefs( lista );
		if( vista != null )
			vista.setVisibility( View.GONE );
	}

	// Elimina una Nota inlinea o condivisa
	// Restituisce un array dei capostipiti modificati
	public static Object[] eliminaNota( Note nota, View vista ) {
		Set<Object> capi;
		if( nota.getId() != null ) {	// nota OBJECT
			// Prima rimuove i ref alla nota con un bel Visitor
			RiferimentiNota eliminatoreNote = new RiferimentiNota( Globale.gc, nota.getId(), true );
			Globale.gc.accept( eliminatoreNote );
			Globale.gc.getNotes().remove( nota );	// ok la rimuove se è un'object note
			capi = eliminatoreNote.capostipiti;
			if( Globale.gc.getNotes().isEmpty() )
				Globale.gc.setNotes( null );
		} else { // nota LOCALE
			new TrovaPila( Globale.gc, nota );
			NoteContainer nc = (NoteContainer) Memoria.oggettoContenitore();
			nc.getNotes().remove( nota ); // rimuove solo se è una nota locale, non se object note
			if( nc.getNotes().isEmpty() )
				nc.setNotes( null );
			capi = new HashSet<>();
			capi.add( Memoria.oggettoCapo() );
			Memoria.arretra();
		}
		Memoria.annullaIstanze( nota );
		if( vista != null )
			vista.setVisibility( View.GONE );
		return capi.toArray();
	}

	// Elenca tutti i media di un oggetto contenitore
	public static void mettiMedia( LinearLayout scatola, Object contenitore, boolean dettagli ) {
		RecyclerView griglia = new AdattatoreGalleriaMedia.RiciclaVista( scatola.getContext(), dettagli );
		griglia.setHasFixedSize( true );
		RecyclerView.LayoutManager gestoreLayout = new GridLayoutManager( scatola.getContext(), dettagli?2:3 );
		griglia.setLayoutManager( gestoreLayout );
		List<ListaMediaContenitore.MedCont> listaMedia = new ArrayList<>();
		for( Media med : ((MediaContainer)contenitore).getAllMedia(Globale.gc) )
			listaMedia.add( new ListaMediaContenitore.MedCont(med,contenitore) );
		AdattatoreGalleriaMedia adattatore = new AdattatoreGalleriaMedia( listaMedia, dettagli );
		griglia.setAdapter( adattatore );
		scatola.addView( griglia );
	}

	// Di un oggetto inserisce le citazioni alle fonti
	public static void citaFonti( LinearLayout scatola, Object contenitore ) {
		if( Globale.preferenze.esperto ) {
			List<SourceCitation> listaCitaFonti;
			if( contenitore instanceof Note )	// Note non estende SourceCitationContainer
				listaCitaFonti = ( (Note) contenitore ).getSourceCitations();
			else listaCitaFonti = ((SourceCitationContainer)contenitore).getSourceCitations();
			for( final SourceCitation citaz : listaCitaFonti ) {
				View vistaCita = LayoutInflater.from( scatola.getContext() ).inflate( R.layout.pezzo_citazione_fonte, scatola, false );
				scatola.addView( vistaCita );
				if( citaz.getSource(Globale.gc) != null )	// source CITATION
					((TextView)vistaCita.findViewById( R.id.fonte_testo )).setText( Biblioteca.titoloFonte(citaz.getSource(Globale.gc)) );
				else // source NOTE, oppure Citazione di fonte che è stata eliminata
					vistaCita.findViewById( R.id.citazione_fonte ).setVisibility( View.GONE );
				String t = "";
				if( citaz.getValue() != null ) t += citaz.getValue() + "\n";
				if( citaz.getPage() != null ) t += citaz.getPage() + "\n";
				if( citaz.getDate() != null ) t += citaz.getDate() + "\n";
				if( citaz.getText() != null ) t += citaz.getText() + "\n";	// vale sia per sourceNote che per sourceCitation
				TextView vistaTesto = vistaCita.findViewById( R.id.citazione_testo );
				if( t.isEmpty() ) vistaTesto.setVisibility( View.GONE );
				else vistaTesto.setText( t.substring( 0, t.length() - 1 ) );
				// Tutto il resto
				LinearLayout scatolaAltro = vistaCita.findViewById( R.id.citazione_note );
				mettiNote( scatolaAltro, citaz, false );
				mettiMedia( scatolaAltro, citaz, false );
				vistaCita.setTag( R.id.tag_oggetto, citaz );
				if( scatola.getContext() instanceof Individuo ) { // Fragment individuoEventi
					((AppCompatActivity)scatola.getContext()).getSupportFragmentManager()
							.findFragmentByTag( "android:switcher:" + R.id.schede_persona + ":1" )
							.registerForContextMenu( vistaCita );
				} else	// AppCompatActivity
					((AppCompatActivity)scatola.getContext()).registerForContextMenu( vistaCita );

				vistaCita.setOnClickListener( v -> {
					Intent intento = new Intent( scatola.getContext(), CitazioneFonte.class );
					Memoria.aggiungi( citaz );
					scatola.getContext().startActivity( intento );
				});
			}
		}
	}

	// Inserisce nella scatola il richiamo ad una fonte, con dettagli o essenziale
	public static void mettiFonte( final LinearLayout scatola, final Source fonte, boolean dettagli ) {
		View vistaFonte = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_fonte, scatola, false );
		scatola.addView( vistaFonte );
		TextView vistaTesto = vistaFonte.findViewById( R.id.fonte_testo );
		String txt = "";
		if( dettagli ) {
			if( fonte.getTitle() != null )
				txt = fonte.getTitle() + "\n";
			else if( fonte.getAbbreviation() != null )
				txt = fonte.getAbbreviation() + "\n";
			if( fonte.getType() != null )
				txt += fonte.getType().replaceAll("\n", " ") + "\n";
			if( fonte.getPublicationFacts() != null )
				txt += fonte.getPublicationFacts().replaceAll("\n", " ") + "\n";
			if( fonte.getText() != null )
				txt += fonte.getText().replaceAll("\n", " ");
			if( txt.endsWith("\n") )
				txt = txt.substring( 0, txt.length()-1 );
			LinearLayout scatolaAltro = vistaFonte.findViewById( R.id.fonte_scatola );
			mettiNote( scatolaAltro, fonte, false );
			mettiMedia( scatolaAltro, fonte, false );
			vistaFonte.setTag( R.id.tag_oggetto, fonte );
			((AppCompatActivity)scatola.getContext()).registerForContextMenu( vistaFonte );
		} else {
			vistaTesto.setMaxLines( 2 );
			txt = Biblioteca.titoloFonte(fonte);
		}
		vistaTesto.setText( txt );
		vistaFonte.setOnClickListener( v -> {
			Memoria.setPrimo( fonte );
			scatola.getContext().startActivity( new Intent( scatola.getContext(), Fonte.class) );
		});
	}

	// La view ritornata è usata da Condivisione
	public static View linkaPersona( LinearLayout scatola, Person p, int scheda ) {
		View vistaPersona = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_individuo_piccolo, scatola, false );
		scatola.addView( vistaPersona );
		F.unaFoto( Globale.gc, p, vistaPersona.findViewById(R.id.collega_foto) );
		((TextView)vistaPersona.findViewById( R.id.collega_nome )).setText( epiteto(p) );
		String dati = dueAnni( p, false );
		TextView vistaDettagli = vistaPersona.findViewById( R.id.collega_dati );
		if( dati.isEmpty() ) vistaDettagli.setVisibility( View.GONE );
		else vistaDettagli.setText( dati );
		if( !morto( p ) )
			vistaPersona.findViewById( R.id.collega_lutto ).setVisibility( View.GONE );
		if( sesso(p) == 1 )
			vistaPersona.findViewById(R.id.collega_bordo).setBackgroundResource( R.drawable.casella_bordo_maschio );
		if( sesso(p) == 2 )
			vistaPersona.findViewById(R.id.collega_bordo).setBackgroundResource( R.drawable.casella_bordo_femmina );
		vistaPersona.setOnClickListener( v -> {
			Memoria.setPrimo( p );
			Intent intento = new Intent( scatola.getContext(), Individuo.class );
			intento.putExtra( "scheda", scheda );
			scatola.getContext().startActivity( intento );
		} );
		return vistaPersona;
	}

	static String testoFamiglia( Context contesto, Gedcom gc, Family fam, boolean unaLinea ) {
		String testo = "";
		for( Person marito : fam.getHusbands(gc) )
			testo += epiteto( marito ) + "\n";
		for( Person moglie : fam.getWives(gc) )
			testo += epiteto( moglie ) + "\n";
		if( fam.getChildren(gc).size() == 1 ) {
			testo += epiteto( fam.getChildren(gc).get(0) );
		} else if( fam.getChildren(gc).size() > 1 )
			testo += contesto.getString(R.string.num_children, fam.getChildren(gc).size());
		if( testo.endsWith("\n") ) testo = testo.substring( 0, testo.length()-1 );
		if( unaLinea )
			testo = testo.replaceAll( "\n", ", " );
		if( testo.isEmpty() )
			testo = "[" + contesto.getString(R.string.empty_family) + "]";
		return testo;
	}

	// Usato da dispensa
	static void linkaFamiglia( LinearLayout scatola, Family fam ) {
		View vistaFamiglia = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_famiglia_piccolo, scatola, false );
		scatola.addView( vistaFamiglia );
		((TextView)vistaFamiglia.findViewById( R.id.famiglia_testo )).setText( testoFamiglia(scatola.getContext(),Globale.gc,fam,false) );
		vistaFamiglia.setOnClickListener( v -> {
			Memoria.setPrimo( fam );
			scatola.getContext().startActivity( new Intent( scatola.getContext(), Famiglia.class ) );
		});
	}

	// Usato da dispensa
	static void linkaMedia( LinearLayout scatola, Media media ) {
		View vistaMedia = LayoutInflater.from(scatola.getContext()).inflate( R.layout.pezzo_media, scatola, false );
		scatola.addView( vistaMedia );
		AdattatoreGalleriaMedia.arredaMedia( media, vistaMedia.findViewById(R.id.media_testo), vistaMedia.findViewById(R.id.media_num) );
		LinearLayout.LayoutParams parami = (LinearLayout.LayoutParams)vistaMedia.getLayoutParams();
		parami.height = dpToPx( 80 );
		F.dipingiMedia( media, vistaMedia.findViewById(R.id.media_img), vistaMedia.findViewById(R.id.media_circolo) );
		vistaMedia.setOnClickListener( v -> {
			Memoria.setPrimo( media );
			scatola.getContext().startActivity( new Intent( scatola.getContext(), Immagine.class) );
		} );
	}

	// Aggiunge un autore al layout
	static void linkAutore( LinearLayout scatola, Submitter autor ) {
		Context contesto = scatola.getContext();
		View vista = LayoutInflater.from(contesto).inflate( R.layout.pezzo_nota, scatola, false);
		scatola.addView( vista );
		TextView testoNota = vista.findViewById( R.id.nota_testo );
		testoNota.setText( autor.getName() );
		vista.findViewById( R.id.nota_fonti ).setVisibility( View.GONE );
		vista.setOnClickListener( v -> {
			Memoria.setPrimo( autor );
			contesto.startActivity( new Intent( contesto, Autore.class ) );
		});
	}

	// Aggiunge al layout un contenitore generico con uno o più collegamenti a record capostipiti
	public static void mettiDispensa( LinearLayout scatola, Object cosa, int tit ) {
		View vista = LayoutInflater.from(scatola.getContext()).inflate( R.layout.dispensa, scatola, false );
		TextView vistaTit = vista.findViewById( R.id.dispensa_titolo );
		vistaTit.setText( tit );
		vistaTit.setBackground( AppCompatResources.getDrawable(scatola.getContext(),R.drawable.sghembo) ); // per android 4
		scatola.addView( vista );
		LinearLayout dispensa = vista.findViewById( R.id.dispensa_scatola );
		if( cosa instanceof Object[] ) {
			for( Object o : (Object[])cosa )
				mettiQualsiasi( dispensa, o );
		} else
			mettiQualsiasi( dispensa, cosa );
	}

	// Riconosce il tipo di record e aggiunge il link appropriato alla scatola
	static void mettiQualsiasi( LinearLayout scatola, Object record ) {
		if( record instanceof Person )
			linkaPersona( scatola, (Person)record, 1 );
		else if( record instanceof Source )
			mettiFonte( scatola, (Source)record, false );
		else if( record instanceof Family )
			linkaFamiglia( scatola, (Family)record );
		else if( record instanceof Repository )
			ArchivioRef.mettiArchivio( scatola, (Repository)record );
		else if( record instanceof Note )
			mettiNota( scatola, (Note)record, true );
		else if( record instanceof Media )
			linkaMedia( scatola, (Media)record );
		else if( record instanceof Submitter )
			linkAutore( scatola, (Submitter)record );
	}

	// Aggiunge al layout il pezzo con la data e tempo di Cambiamento
	public static View cambiamenti( final LinearLayout scatola, final Change cambi ) {
		View vistaCambio = null;
		if( cambi != null && Globale.preferenze.esperto ) {
			vistaCambio = LayoutInflater.from( scatola.getContext() ).inflate( R.layout.pezzo_data_cambiamenti, scatola, false );
			scatola.addView( vistaCambio );
			TextView vistaTesto = vistaCambio.findViewById( R.id.cambi_testo );
			String dataOra = cambi.getDateTime().getValue() + " - " + cambi.getDateTime().getTime();
			if( dataOra.isEmpty() ) vistaTesto.setVisibility( View.GONE );
			else vistaTesto.setText( dataOra );
			LinearLayout scatolaNote = vistaCambio.findViewById( R.id.cambi_note );
			for( Estensione altroTag : trovaEstensioni( cambi ) )
				metti( scatolaNote, altroTag.nome, altroTag.testo );
			// Grazie al mio contributo la data cambiamento può avere delle note
			mettiNote( scatolaNote, cambi, false );
			vistaCambio.setOnClickListener( v -> {
				Memoria.aggiungi( cambi );
				scatola.getContext().startActivity( new Intent( scatola.getContext(), Cambiamenti.class ) );
			});
		}
		return vistaCambio;
	}

	// Chiede conferma di eliminare un elemento
	public static boolean preserva( Object cosa ) {
		// todo Conferma elimina
		return false;
	}

	// Restituisce un DateTime con data e ora aggiornate
	public static DateTime dataTempoAdesso() {
		DateTime dataTempo = new DateTime();
		Date now = new Date();
		dataTempo.setValue( String.format(Locale.ENGLISH,"%te %<Tb %<tY",now) );
		dataTempo.setTime( String.format(Locale.ENGLISH,"%tT",now) );
		return dataTempo;
	}

	// Aggiorna la data di cambiamento del/dei record
	public static void aggiornaDate( Object ... oggetti ) {
		for( Object aggiornando : oggetti ) {
			try { // se aggiornando non ha il metodo get/setChange, passa oltre silenziosamente
				Change chan = (Change)aggiornando.getClass().getMethod( "getChange" ).invoke( aggiornando );
				if( chan == null ) // il record non ha ancora un CHAN
					chan = new Change();
				chan.setDateTime( dataTempoAdesso() );
				aggiornando.getClass().getMethod( "setChange", Change.class ).invoke( aggiornando, chan );
				// Estensione con l'id della zona, una stringa tipo 'America/Sao_Paulo'
				chan.putExtension( "zona", TimeZone.getDefault().getID() );
			} catch( Exception e ) {}
		}
	}

	// Eventualmente salva il Json
	public static void salvaJson( boolean rinfresca, Object ... oggetti ) {
		if( oggetti != null )
			aggiornaDate( oggetti );
		if( rinfresca )
			Globale.editato = true;

		// al primo salvataggio marchia gli autori
		if( Globale.preferenze.alberoAperto().grado == 9 ) {
			for( Submitter autore : Globale.gc.getSubmitters() )
				autore.putExtension( "passato", true );
			Globale.preferenze.alberoAperto().grado = 10;
			Globale.preferenze.salva();
		}

		if( Globale.preferenze.autoSalva )
			salvaJson( Globale.gc, Globale.preferenze.idAprendo );
		else { // mostra il tasto Salva
			Globale.daSalvare = true;
			if( Globale.vistaPrincipe != null ) {
				NavigationView menu = Globale.vistaPrincipe.findViewById(R.id.menu);
				menu.getHeaderView(0).findViewById( R.id.menu_salva ).setVisibility( View.VISIBLE );
			}
		}
	}

	static void salvaJson( Gedcom gc, int idAlbero ) {
		Header h = gc.getHeader();
		// Solo se l'header è di Family Gem
		if( h != null && h.getGenerator() != null
				&& h.getGenerator().getValue() != null && h.getGenerator().getValue().equals("FAMILY_GEM") ) {
			// Aggiorna la data e l'ora
			h.setDateTime( dataTempoAdesso() );
			// Eventualmente aggiorna la versione di Family Gem
			if( (h.getGenerator().getVersion() != null && !h.getGenerator().getVersion().equals(BuildConfig.VERSION_NAME))
					|| h.getGenerator().getVersion() == null )
				h.getGenerator().setVersion( BuildConfig.VERSION_NAME );
		}
		try {
			FileUtils.writeStringToFile(
					new File( Globale.contesto.getFilesDir(), idAlbero + ".json" ),
					new JsonParser().toJson( gc ), "UTF-8"
			);
		} catch (IOException e) {
			Toast.makeText( Globale.contesto, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
		}
	}

	static int castaJsonInt( Object ignoto ) {
		if( ignoto instanceof Integer ) return (int) ignoto;
		else return ((JsonPrimitive)ignoto).getAsInt();
	}

	static String castaJsonString( Object ignoto ) {
		if( ignoto == null ) return null;
		else if( ignoto instanceof String ) return (String) ignoto;
		else return ((JsonPrimitive)ignoto).getAsString();
	}

	static int dpToPx(float dips) {
		return (int) (dips * Globale.contesto.getResources().getDisplayMetrics().density + 0.5f);
	}

	// Valuta se ci sono individui collegabili rispetto a un individuo.
	// Usato per decidere se far comparire 'Collega persona esistente' nel menu
	static boolean ciSonoIndividuiCollegabili( Person piolo ) {
		int numTotali = Globale.gc.getPeople().size();
		if( numTotali > 0 && ( Globale.preferenze.esperto // gli esperti possono sempre
				|| piolo == null ) ) // in una famiglia vuota unRappresentanteDellaFamiglia è null
			return true;
		int numFamili = Anagrafe.quantiFamiliari( piolo );
		return numTotali > numFamili+1;
	}

	// Chiede se referenziare un autore nell'header
	static void autorePrincipale( Context contesto, final String idAutore ) {
		final Header[] testa = { Globale.gc.getHeader() };
		if( testa[0] == null || testa[0].getSubmitterRef() == null ) {
			new AlertDialog.Builder( contesto ).setMessage( R.string.make_main_submitter )
					.setPositiveButton( android.R.string.yes, (dialog, id) -> {
						if( testa[0] == null ) {
							testa[0] = AlberoNuovo.creaTestata( Globale.preferenze.idAprendo+".json" );
							Globale.gc.setHeader(testa[0]);
						}
						testa[0].setSubmitterRef( idAutore );
						salvaJson( true );
					}).setNegativeButton( R.string.no, null ).show();
		}
	}

	// Restituisce il primo autore non passato
	static Submitter autoreFresco( Gedcom gc ) {
		for( Submitter autore : gc.getSubmitters() ) {
			if( autore.getExtension( "passato" ) == null )
				return autore;
		}
		return null;
	}

	// Verifica se un autore ha partecipato alle condivisioni, per non farlo eliminare
	static boolean autoreHaCondiviso( Submitter autore ) {
		List<Armadio.Invio> condivisioni = Globale.preferenze.alberoAperto().condivisioni;
		boolean inviatore = false;
		if( condivisioni != null )
			for( Armadio.Invio invio : condivisioni )
				if( autore.getId().equals( invio.submitter ) )
					inviatore = true;
		return inviatore;
	}

	// Elenco di stringhe dei membri rappresentativi delle famiglie
	static String[] elencoFamiglie(List<Family> listaFamiglie) {
		List<String> famigliePerno = new ArrayList<>();
		for( Family fam : listaFamiglie ) {
			String etichetta = testoFamiglia(Globale.contesto, Globale.gc, fam, true);
			famigliePerno.add( etichetta );
		}
		return famigliePerno.toArray(new String[0]);
	}

	/* Per un perno che è figlio in più di una famiglia chiede quale famiglia mostrare
	cosaAprire:
		0 diagramma della famiglia precedente, senza chiedere quale famiglia (primo click su Diagram)
		1 diagramma chiedendo eventualmente quale famiglia
		2 famiglia chiedendo eventualmente quale famiglia
	 */
	public static void qualiGenitoriMostrare( Context contesto, Person perno, int cosaAprire ) {
		if( perno == null )
			concludiSceltaGenitori( contesto, null, 1, 0 );
		else {
			List<Family> famiglie = perno.getParentFamilies(Globale.gc);
			if( famiglie.size() > 1 && cosaAprire > 0 ) {
				new AlertDialog.Builder( contesto ).setTitle( R.string.which_family )
						.setItems( elencoFamiglie(famiglie), (dialog, quale) -> {
							concludiSceltaGenitori( contesto, perno, cosaAprire, quale );
						}).show();
			} else
				concludiSceltaGenitori( contesto, perno, cosaAprire, 0 );
		}

	}
	private static void concludiSceltaGenitori( Context contesto, Person perno, int cosaAprire, int qualeFamiglia ) {
		if( perno != null )
			Globale.individuo = perno.getId();
		if( cosaAprire > 0 ) // Viene impostata la famiglia da mostrare
			Globale.numFamiglia = qualeFamiglia; // normalmente è la 0
		if( cosaAprire < 2 ) { // Mostra il diagramma
			if( contesto instanceof Principe ) { // Diagram, Anagrafe o Principe stesso
				FragmentManager fm = ((AppCompatActivity)contesto).getSupportFragmentManager();
				// Nome del frammento precedente nel backstack
				String previousName = fm.getBackStackEntryAt( fm.getBackStackEntryCount() - 1 ).getName();
				if( previousName != null && previousName.equals("diagram") )
					fm.popBackStack(); // Ricliccando su Diagram rimuove dalla storia il frammento di diagramma predente
				fm.beginTransaction().replace(R.id.contenitore_fragment, new Diagram()).addToBackStack("diagram").commit();
			} else { // Da individuo o da famiglia
				contesto.startActivity( new Intent( contesto, Principe.class ) );
			}
		} else { // Viene mostrata la famiglia
			Family family = perno.getParentFamilies(Globale.gc).get(qualeFamiglia);
			if( contesto instanceof Famiglia ) { // Passando di Famiglia in Famiglia non accumula attività nello stack
				Memoria.replacePrimo( family );
				((Activity)contesto).recreate();
			} else {
				Memoria.setPrimo( family );
				contesto.startActivity( new Intent( contesto, Famiglia.class ) );
			}
		}
	}

	// Per un perno che ha molteplici matrimoni chiede quale mostrare
	public static void qualiConiugiMostrare(Context contesto, Person perno, Family famiglia) {
		if( perno.getSpouseFamilies(Globale.gc).size() > 1 && famiglia == null ) {
			new AlertDialog.Builder( contesto ).setTitle( R.string.which_family )
					.setItems( elencoFamiglie(perno.getSpouseFamilies(Globale.gc)), (dialog, quale) -> {
						concludiSceltaConiugi( contesto, perno, null, quale );
					}).show();
		} else {
			concludiSceltaConiugi( contesto, perno, famiglia, 0 );
		}
	}
	private static void concludiSceltaConiugi(Context contesto, Person perno, Family famiglia, int quale) {
		Globale.individuo = perno.getId();
		famiglia = famiglia == null ? perno.getSpouseFamilies(Globale.gc).get(quale) : famiglia;
		if( contesto instanceof Famiglia ) {
			Memoria.replacePrimo( famiglia );
			((Activity)contesto).recreate(); // Non accumula activity nello stack
		} else {
			Memoria.setPrimo( famiglia );
			contesto.startActivity( new Intent( contesto, Famiglia.class ) );
		}
	}

	// Usato per collegare una persona ad un'altra, solo in modalità inesperto
	// Verifica se il perno potrebbe avere o ha molteplici matrimoni e chiede a quale attaccare un coniuge o un figlio
	// È anche responsabile di settare 'idFamiglia' oppure 'collocazione'
	static boolean controllaMultiMatrimoni( Intent intento, Context contesto, Fragment frammento ) {
		String idPerno = intento.getStringExtra( "idIndividuo" );
		Person perno = Globale.gc.getPerson(idPerno);
		List<Family> famGenitori = perno.getParentFamilies(Globale.gc);
		List<Family> famSposi = perno.getSpouseFamilies(Globale.gc);
		int relazione = intento.getIntExtra( "relazione", 0 );
		ArrayAdapter<NuovoParente.VoceFamiglia> adapter = new ArrayAdapter<>(contesto, android.R.layout.simple_list_item_1);

		// Genitori: esiste già una famiglia che abbia almeno uno spazio vuoto
		if( relazione == 1 && famGenitori.size() == 1
				&& (famGenitori.get(0).getHusbandRefs().isEmpty() || famGenitori.get(0).getWifeRefs().isEmpty()) )
				intento.putExtra( "idFamiglia", famGenitori.get(0).getId() ); // aggiunge 'idFamiglia' all'intent esistente
		// se questa famiglia è già piena di genitori, 'idFamiglia' rimane null
		// quindi verrà cercata la famiglia esistente del destinatario oppure si crearà una famiglia nuova

		// Genitori: esistono più famiglie
		if( relazione == 1 && famGenitori.size() > 1 ) {
			for( Family fam : famGenitori )
				if( fam.getHusbandRefs().isEmpty() || fam.getWifeRefs().isEmpty() )
					adapter.add( new NuovoParente.VoceFamiglia(contesto,fam) );
			if( adapter.getCount() == 1 )
				intento.putExtra( "idFamiglia", adapter.getItem(0).famiglia.getId() );
			else if( adapter.getCount() > 1 ) {
				new AlertDialog.Builder(contesto).setTitle( R.string.which_family_add_parent )
						.setAdapter( adapter, (dialog, quale) -> {
							intento.putExtra( "idFamiglia", adapter.getItem(quale).famiglia.getId() );
							concludiMultiMatrimoni(contesto, intento, frammento);
						}).show();
				return true;
			}
		}
		// Fratello
		else if( relazione == 2 && famGenitori.size() == 1 ) {
			intento.putExtra( "idFamiglia", famGenitori.get(0).getId() );
		} else if( relazione == 2 && famGenitori.size() > 1 ) {
			new AlertDialog.Builder(contesto).setTitle( R.string.which_family_add_sibling )
					.setItems( elencoFamiglie(famGenitori), (dialog, quale) -> {
						intento.putExtra( "idFamiglia", famGenitori.get(quale).getId() );
						concludiMultiMatrimoni(contesto, intento, frammento);
					}).show();
			return true;
		}
		// Coniuge
		else if( relazione == 3 && famSposi.size() == 1 ) {
			if( famSposi.get(0).getHusbandRefs().isEmpty() || famSposi.get(0).getWifeRefs().isEmpty() ) // Se c'è uno slot libero
				intento.putExtra( "idFamiglia", famSposi.get(0).getId() );
		} else if( relazione == 3 && famSposi.size() > 1 ) {
			for( Family fam : famSposi ) {
				if( fam.getHusbandRefs().isEmpty() || fam.getWifeRefs().isEmpty() )
					adapter.add( new NuovoParente.VoceFamiglia(contesto,fam) );
			}
			// Nel caso di zero famiglie papabili, idFamiglia rimane null
			if( adapter.getCount() == 1 ) {
				intento.putExtra( "idFamiglia", adapter.getItem(0).famiglia.getId() );
			} else if( adapter.getCount() > 1 ) {
				//adapter.add(new NuovoParente.VoceFamiglia(contesto,perno) );
				new AlertDialog.Builder(contesto).setTitle( R.string.which_family_add_spouse )
						.setAdapter( adapter, (dialog, quale) -> {
							intento.putExtra( "idFamiglia", adapter.getItem(quale).famiglia.getId() );
							concludiMultiMatrimoni(contesto, intento, frammento);
						}).show();
				return true;
			}
		}
		// Figlio: esiste già una famiglia con o senza figli
		else if( relazione == 4 && famSposi.size() == 1 ) {
			intento.putExtra( "idFamiglia", famSposi.get(0).getId() );
		} // Figlio: esistono molteplici famiglie coniugali
		else if( relazione == 4 && famSposi.size() > 1 ) {
			new AlertDialog.Builder(contesto).setTitle( R.string.which_family_add_child )
					.setItems( elencoFamiglie(famSposi), (dialog, quale) -> {
						intento.putExtra( "idFamiglia", famSposi.get(quale).getId() );
						concludiMultiMatrimoni(contesto, intento, frammento);
					}).show();
			return true;
		}
		// Non avendo trovato una famiglia di perno, dice ad Anagrafe di cercare di collocare perno nella famiglia del destinatario
		if( intento.getStringExtra("idFamiglia") == null && intento.getBooleanExtra("anagrafeScegliParente", false) )
			intento.putExtra( "collocazione", "FAMIGLIA_ESISTENTE" );
		return false;
	}

	// Conclusione della funzione precedente
	static void concludiMultiMatrimoni(Context contesto, Intent intento, Fragment frammento) {
		if( intento.getBooleanExtra( "anagrafeScegliParente", false ) ) {
			// apre Anagrafe
			if( frammento != null )
				frammento.startActivityForResult( intento,1401 );
			else
				((Activity)contesto).startActivityForResult( intento,1401 );
		} else // apre EditaIndividuo
			contesto.startActivity( intento );
	}

	// Controlla che una o più famiglie siano vuote e propone di eliminarle
	// 'ancheKo' dice di eseguire 'cheFare' anche cliccando Cancel o fuori dal dialogo
	static boolean controllaFamiglieVuote(Context contesto, Runnable cheFare, boolean ancheKo, Family... famiglie) {
		List<Family> vuote = new ArrayList<>();
		for( Family fam : famiglie ) {
			int membri = fam.getHusbandRefs().size() + fam.getWifeRefs().size() + fam.getChildRefs().size();
			if( membri <= 1 && fam.getEventsFacts().isEmpty() && fam.getAllMedia(Globale.gc).isEmpty()
					&& fam.getAllNotes(Globale.gc).isEmpty() && fam.getSourceCitations().isEmpty() ) {
				vuote.add(fam);
			}
		}
		if( vuote.size() > 0 ) {
			new AlertDialog.Builder(contesto).setMessage( R.string.empty_family_delete )
					.setPositiveButton(android.R.string.yes, (dialog, i) -> {
						for(Family f : vuote)
							Chiesa.eliminaFamiglia( f.getId() ); // Così capita di salvare più volte insieme... ma vabè
						if(cheFare != null) cheFare.run();
					}).setNeutralButton(android.R.string.cancel, (dialog, i) -> {
						if(ancheKo) cheFare.run();
					}).setOnCancelListener( dialog -> {
						if(ancheKo) cheFare.run();
					}).show();
			return true;
		}
		return false;
	}

	// Mostra un messaggio Toast anche da un thread collaterale
	static void tosta( Activity contesto, int messaggio ) {
		tosta( contesto, contesto.getString( messaggio ) );
	}
	static void tosta( Activity contesto, String messaggio ) {
		contesto.runOnUiThread( () -> Toast.makeText( contesto, messaggio, Toast.LENGTH_LONG ).show() );
	}
}