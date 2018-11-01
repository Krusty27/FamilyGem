package app.familygem;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.WindowCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import app.familygem.dettaglio.Autore;

public class Principe extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

	DrawerLayout scatolissima;

	@Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.principe);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

		scatolissima = findViewById(R.id.scatolissima);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, scatolissima, toolbar, R.string.drawer_open, R.string.drawer_close );
		scatolissima.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView menuPrincipe = findViewById(R.id.menu);
		menuPrincipe.setNavigationItemSelectedListener(this);
		Globale.vistaPrincipe = scatolissima;
		arredaTestataMenu();

        if( savedInstanceState == null ) {  // carica la home solo la prima volta, non ruotando lo schermo
			//s.l( "Globale.preferenze.idAprendo = " + Globale.preferenze.idAprendo );
			if( Globale.preferenze.idAprendo == 0 )	// cioè praticamente alla prima apertura
				startActivity( new Intent(this, AlberoNuovo.class) );
			else if( getIntent().getBooleanExtra("anagrafeScegliParente",false) )
				getSupportFragmentManager().beginTransaction().replace(R.id.contenitore_fragment, new Anagrafe()).commit();
			else if( getIntent().getBooleanExtra("galleriaScegliMedia",false) )
				getSupportFragmentManager().beginTransaction().replace(R.id.contenitore_fragment, new Galleria()).commit();
			else if( getIntent().getBooleanExtra("bibliotecaScegliFonte",false) )
				getSupportFragmentManager().beginTransaction().replace(R.id.contenitore_fragment, new Biblioteca()).commit();
			else if( getIntent().getBooleanExtra("quadernoScegliNota",false) )
				getSupportFragmentManager().beginTransaction().replace(R.id.contenitore_fragment, new Quaderno()).commit();
			else if( getIntent().getBooleanExtra("magazzinoScegliArchivio",false) )
				getSupportFragmentManager().beginTransaction().replace(R.id.contenitore_fragment, new Magazzino()).commit();
			else {    // la normale apertura
				/*if( Globale.gc == null ) {
					//Alberi.apriJson(Globale.preferenze.getString("albero_apertura", null));
					Alberi.apriJson(Globale.preferenze.idAprendo);
				}*/
				getSupportFragmentManager().beginTransaction().replace(R.id.contenitore_fragment, new Diagramma()).commit();
			}
        }

		NavigationView menu = scatolissima.findViewById(R.id.menu);
		menu.getHeaderView(0).findViewById( R.id.menu_testa ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( View v ) {
				/*FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace( R.id.contenitore_fragment, new Alberi() );
				ft.addToBackStack(null).commit();*/
				scatolissima.closeDrawer(GravityCompat.START);
				startActivity( new Intent( Principe.this, Alberi.class ) );
			}
		} );
    }

	// Aggiorna i contenuti quando si torna indietro con backPressed()
	@Override
	public void onRestart() {
		//s.l("Principe onRestart " + Globale.editato + "  isTaskRoot " + isTaskRoot() );
		super.onRestart();
		if( Globale.editato ) {
			recreate();
			Globale.editato = false;
		}
	}

	// Titolo e immagine a caso del Gedcom
	static void arredaTestataMenu() {
		NavigationView menu = Globale.vistaPrincipe.findViewById(R.id.menu);
		ImageView immagine = menu.getHeaderView(0).findViewById( R.id.menu_immagine );
		TextView testo = menu.getHeaderView(0).findViewById( R.id.menu_titolo );
		immagine.setVisibility( ImageView.GONE );
		testo.setText( "" );
		if( Globale.gc != null ) {
			/*String percorso = "";
			esterno:
			for( Person uno : Globale.gc.getPeople() ) {
				for (Media media : uno.getAllMedia(Globale.gc)) {
					percorso = U.percorsoMedia(media);
					if (percorso != null)
						break esterno;
				}
			}
			immagine.setImageBitmap( BitmapFactory.decodeFile(percorso) );*/
			VisitaListaMedia visitaMedia = new VisitaListaMedia(true);
			Globale.gc.accept( visitaMedia );
			if( visitaMedia.listaMedia.size() > 0 ) {
				List<Media> lista = new ArrayList<>( visitaMedia.listaMedia.keySet() );
				Random caso = new Random();
				int num = caso.nextInt( lista.size() );
				U.mostraMedia( immagine, lista.get(num) );
				//if( immagine.getTag(R.id.tag_file_senza_anteprima) == null ) essendo asincrono arriva in ritardo
				immagine.setVisibility( ImageView.VISIBLE );
				//s.l( num + "  " + immagine.getTag(R.id.tag_file_senza_anteprima) + "  " + lista.get(num).getFile() );
			}
			testo.setText( Globale.preferenze.alberoAperto().nome );
		}
	}

    @Override
    public void onBackPressed() {
        if( scatolissima.isDrawerOpen(GravityCompat.START) ) {
	        scatolissima.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_secondo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;
        /*if (id == R.id.nav_alberi) {
            fragment = new Alberi();
        } else */
        if (id == R.id.nav_diagramma) {
            fragment = new Diagramma();
        } else if (id == R.id.nav_persone) {
            fragment = new Anagrafe();
        } else if (id == R.id.nav_fonti) {
            fragment = new Biblioteca();
		} else if (id == R.id.nav_archivi) {
			fragment = new Magazzino();
        } else if (id == R.id.nav_media) {
        	// ToDo: Se clicco  IndividuoMedia > FAB > CollegaMedia....  "galleriaScegliMedia" viene passato all'intent dell'activity con valore 'true'
	        // todo: porò rimane true anche quando poi torno in Galleria cliccando nel drawer, con conseguenti errori:
	        // vengono visualizzati solo gli oggetti media
	        // cliccandone uno esso viene aggiunto ai media dell'ultima persona vista !
            fragment = new Galleria();
		} else if (id == R.id.nav_famiglie) {
			fragment = new Chiesa();
        } else if (id == R.id.nav_note) {
	        fragment = new Quaderno();
        } else if (id == R.id.nav_autore) {
	        fragment = new Podio();
        /*} else if (id == R.id.nav_varie) {
           startActivity( new Intent(this, DiagrammaActivity.class) );
			//startActivity( new Intent(this, Lavagna.class) );
        } else if (id == R.id.nav_officina) {
            startActivity( new Intent(this, Officina.class) );*/
        }
        if( fragment != null ) {
            //getSupportActionBar().setTitle(titolo);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace( R.id.contenitore_fragment, fragment );
            ft.addToBackStack(null);
            ft.commit();
        }
	    scatolissima.closeDrawer(GravityCompat.START);

        return true;
    }
}
