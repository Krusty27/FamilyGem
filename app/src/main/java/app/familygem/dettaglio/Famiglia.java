package app.familygem.dettaglio;

import android.content.Intent;
import android.view.View;
import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import app.familygem.Dettaglio;
import app.familygem.EditaIndividuo;
import app.familygem.Globale;
import app.familygem.Individuo;
import app.familygem.Memoria;
import app.familygem.R;
import app.familygem.U;
import static app.familygem.Globale.gc;

public class Famiglia extends Dettaglio {

	Family f;

	@Override
	public void impagina() {
		setTitle( R.string.family );
		f = (Family) casta( Family.class );
		mettiBava( "FAM", f.getId() );
		for( SpouseRef refMarito : f.getHusbandRefs() )
			membro( refMarito, !f.getChildRefs().isEmpty() ? 1 : 2 );
		for( SpouseRef refMoglie : f.getWifeRefs() )
			membro( refMoglie, !f.getChildRefs().isEmpty() ? 1 : 2 );
		for( EventFact ef : f.getEventsFacts() ) {
			if( ef.getTag().equals("MARR") )
				metti( getString(R.string.marriage), ef );
		}
		for( ChildRef refFiglio : f.getChildRefs() )
			membro( refFiglio, 3 );
		for( EventFact ef : f.getEventsFacts() ) {
			if( !ef.getTag().equals( "MARR" ) )
				metti( ef.getDisplayType(), ef );
		}
		mettiEstensioni( f );
		U.mettiNote( box, f, true );
		U.mettiMedia( box, f, true );
		U.citaFonti( box, f );
		U.cambiamenti( box, f.getChange() );
	}

	void membro( SpouseRef sr, int relazione ) {
		Person p = sr.getPerson(gc);
		int ruolo = 0;
		if( U.sesso(p) == 1 ) {
			switch( relazione ) {
				case 1: ruolo = R.string.father; break;
				case 2: ruolo = R.string.husband; break;
				case 3: ruolo = R.string.son;
			}
		} else if( U.sesso(p) == 2 ) {
			switch( relazione ) {
				case 1: ruolo = R.string.mother; break;
				case 2: ruolo = R.string.wife; break;
				case 3: ruolo = R.string.daughter;
			}
		} else {
			switch( relazione ) {
				case 1: ruolo = R.string.parent; break;
				case 2: ruolo = R.string.spouse; break;
				case 3: ruolo = R.string.child;
			}
		}
		View vistaPersona = U.mettiIndividuo( box, p, getString(ruolo) );
		vistaPersona.setTag( R.id.tag_oggetto, p ); // per il menu contestuale in Dettaglio
		/*  Ref nell'individuo verso la famiglia
			Se la stessa persona è presente più volte con lo stesso ruolo (parent/child) nella stessa famiglia
			i 2 loop seguenti individuano nella person il *primo* FamilyRef (INDI.FAMS / INDI.FAMC) che rimanda a quella famiglia
			Non prendono quello con lo stesso indice del corrispondente Ref nella famiglia  (FAM.HUSB / FAM.WIFE)
			Poteva essere un problema in caso di 'Scollega', ma non più perché tutto il contenuto di Famiglia viene ricaricato
		 */
		if( relazione == 1 || relazione == 2 ) {
			for( SpouseFamilyRef sfr : p.getSpouseFamilyRefs() )
				if( sfr.getRef().equals(f.getId()) ) {
					vistaPersona.setTag( R.id.tag_spouse_family_ref, sfr );
					break;
				}
		} else if( relazione == 3 ) {
			for( ParentFamilyRef pfr : p.getParentFamilyRefs() )
				if( pfr.getRef().equals(f.getId()) ) {
					vistaPersona.setTag( R.id.tag_spouse_family_ref, pfr );
					break;
				}
		}
		vistaPersona.setTag( R.id.tag_spouse_ref, sr );
		registerForContextMenu( vistaPersona );
		vistaPersona.setOnClickListener( v -> {
			List<Family> parentFam = p.getParentFamilies(gc);
			List<Family> spouseFam = p.getSpouseFamilies(gc);
			// un coniuge con una o più famiglie in cui è figlio
			if( (relazione==1 || relazione==2) && !parentFam.isEmpty() ) {
				U.qualiGenitoriMostrare( this, p, 2 );
			} // un figlio con una o più famiglie in cui è coniuge
			else if( relazione == 3 && !p.getSpouseFamilies(gc).isEmpty() ) {
				U.qualiConiugiMostrare( this, p, null );
			} // un figlio non sposato che ha più famiglie genitoriali
			else if( parentFam.size() > 1 ) {
				if( parentFam.size() == 2 ) { // Swappa tra le 2 famiglie genitoriali
					Globale.individuo = p.getId();
					Globale.numFamiglia = parentFam.indexOf(f) == 0 ? 1 : 0;
					Memoria.replacePrimo( parentFam.get(Globale.numFamiglia) );
					recreate();
				} else // Più di due famiglie
					U.qualiGenitoriMostrare( this, p, 2 );
			} // un coniuge senza genitori ma con più famiglie coniugali
			else if( spouseFam.size() > 1 ) {
				if( spouseFam.size() == 2 ) { // Swappa tra le 2 famiglie coniugali
					Globale.individuo = p.getId();
					Family altraFamiglia = spouseFam.get( spouseFam.indexOf(f) == 0 ? 1 : 0 );
					Memoria.replacePrimo( altraFamiglia );
					recreate();
				} else
					U.qualiConiugiMostrare( this, p, null );
			} else {
				Memoria.setPrimo( p );
				startActivity( new Intent(this, Individuo.class) );
			}
		});
		if( unRappresentanteDellaFamiglia == null )
			unRappresentanteDellaFamiglia = p;
	}

	// Collega una persona ad una famiglia come genitore o figlio
	public static void aggrega( Person tizio, Family fam, int ruolo ) {
		switch( ruolo ) {
			case 5:	// Genitore
				// il ref dell'indi nella famiglia
				SpouseRef sr = new SpouseRef();
				sr.setRef( tizio.getId() );
				EditaIndividuo.aggiungiConiuge( fam, sr );

				// il ref della famiglia nell'indi
				SpouseFamilyRef sfr = new SpouseFamilyRef();
				sfr.setRef( fam.getId() );
				//tizio.getSpouseFamilyRefs().add( sfr );	// no: con lista vuota UnsupportedOperationException
				//List<SpouseFamilyRef> listaSfr = tizio.getSpouseFamilyRefs();	// Non va bene:
				// quando la lista è inesistente, anzichè restituire una ArrayList restituisce una Collections$EmptyList che è IMMUTABILE cioè non ammette add()
				List<SpouseFamilyRef> listaSfr = new ArrayList<>( tizio.getSpouseFamilyRefs() );	// ok
				listaSfr.add( sfr );	// ok
				tizio.setSpouseFamilyRefs( listaSfr );
				break;
			case 6:	// Figlio
				ChildRef cr = new ChildRef();
				cr.setRef( tizio.getId() );
				fam.addChild( cr );
				ParentFamilyRef pfr = new ParentFamilyRef();
				pfr.setRef( fam.getId() );
				//tizio.getParentFamilyRefs().add( pfr );	// UnsupportedOperationException
				List<ParentFamilyRef> listaPfr = new ArrayList<>( tizio.getParentFamilyRefs() );
				listaPfr.add( pfr );
				tizio.setParentFamilyRefs( listaPfr );
		}
	}

	// Rimuove il singolo SpouseFamilyRef dall'individuo e il corrispondente SpouseRef dalla famiglia
	public static void scollega( SpouseFamilyRef sfr, SpouseRef sr ) {
		// Dalla persona alla famiglia
		Person pers = sr.getPerson( gc );
		pers.getSpouseFamilyRefs().remove( sfr );
		if( pers.getSpouseFamilyRefs().isEmpty() )
			pers.setSpouseFamilyRefs( null ); // Eventuale lista vuota viene eliminata
		pers.getParentFamilyRefs().remove( sfr );
		if( pers.getParentFamilyRefs().isEmpty() )
			pers.setParentFamilyRefs( null );
		// Dalla famiglia alla persona
		Family fam = sfr.getFamily( gc );
		fam.getHusbandRefs().remove( sr );
		if( fam.getHusbandRefs().isEmpty() )
			fam.setHusbandRefs( null );
		fam.getWifeRefs().remove( sr );
		if( fam.getWifeRefs().isEmpty() )
			fam.setWifeRefs( null );
		fam.getChildRefs().remove( sr );
		if( fam.getChildRefs().isEmpty() )
			fam.setChildRefs( null );
	}

	// Rimuove TUTTI i ref di un individuo in una famiglia
	public static void scollega( String idIndi, Family fam ) {
		// Rimuove i ref dell'indi nella famiglia
		Iterator<SpouseRef> refiSposo = fam.getHusbandRefs().iterator();
		while( refiSposo.hasNext() )
			if( refiSposo.next().getRef().equals(idIndi) )
				refiSposo.remove();
		if( fam.getHusbandRefs().isEmpty() )
			fam.setHusbandRefs( null ); // Elimina eventuale lista vuota

		refiSposo = fam.getWifeRefs().iterator();
		while( refiSposo.hasNext() )
			if( refiSposo.next().getRef().equals(idIndi) )
				refiSposo.remove();
		if( fam.getWifeRefs().isEmpty() )
			fam.setWifeRefs( null );

		Iterator<ChildRef> refiFiglio = fam.getChildRefs().iterator();
		while( refiFiglio.hasNext() )
			if( refiFiglio.next().getRef().equals(idIndi) )
				refiFiglio.remove();
		if( fam.getChildRefs().isEmpty() )
			fam.setChildRefs( null );

		// Rimuove i ref della famiglia nell'indi
		Person person = gc.getPerson(idIndi);
		Iterator<SpouseFamilyRef> iterSfr = person.getSpouseFamilyRefs().iterator();
		while( iterSfr.hasNext() )
			if( iterSfr.next().getRef().equals(fam.getId()) )
				iterSfr.remove();
		if( person.getSpouseFamilyRefs().isEmpty() )
			person.setSpouseFamilyRefs( null );

		Iterator<ParentFamilyRef> iterPfr = person.getParentFamilyRefs().iterator();
		while( iterPfr.hasNext() )
			if( iterPfr.next().getRef().equals(fam.getId()) )
				iterPfr.remove();
		if( person.getParentFamilyRefs().isEmpty() )
			person.setParentFamilyRefs( null );
	}
}