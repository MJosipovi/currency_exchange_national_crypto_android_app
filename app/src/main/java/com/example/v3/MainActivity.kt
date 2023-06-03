package com.example.v3

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.doOnTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    lateinit var drawerLayout: DrawerLayout
    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    private lateinit var dbHelper: MyDatabaseHelper //data base

    fun showToast(context: Context = applicationContext, message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, message, duration).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Bundle u kojem se nalaze postavke prijasnje instance (ako ih ima) - korisno kod rotacije ekrana
        setContentView(R.layout.activity_main) //uzmi activity_main.xml i prikaži ga

        //inicijalizacija svih elemenata UI-a
        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.my_drawer_layout)
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        // to make the Navigation drawer icon always appear on the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //navigation drawer akcije na klik
        val navigationView = findViewById<NavigationView>(R.id.nav_view) //Assuming you have a NavigationView with id 'nav_view'
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.singleConversion -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.aboutApp -> {
                    val intent = Intent(this, AboutAppActivity::class.java)
                    startActivity(intent)
                }
                R.id.exitApp -> {
                    finishAffinity()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START) // Assuming 'drawerLayout' is the ID of your DrawerLayout
            true
        }


        val spinnerIzvornaValuta = findViewById<Spinner>(R.id.spinner)  ?: return // return if the view is null
        val spinnerZeljenaValuta = findViewById<Spinner>(R.id.spinner2)  ?: return // return if the view is null


        val buttonIzracunaj = findViewById<Button>(R.id.buttonIzracunaj)
        val buttonObrisiSadrzaj = findViewById<Button>(R.id.buttonObrisiSadrzaj)
        val buttonZamjeniValute = findViewById<Button>(R.id.buttonZamjeniValute)

        val IV_default_10 = findViewById<Button>(R.id.IV_default_10)
        val IV_default_20 = findViewById<Button>(R.id.IV_default_20)
        val IV_default_30 = findViewById<Button>(R.id.IV_default_30)
        val IV_default_50 = findViewById<Button>(R.id.IV_default_50)
        val IV_default_100 = findViewById<Button>(R.id.IV_default_100)

        val autoIzracunaj = findViewById<Switch>(R.id.autoIzracunajSwitch)

        val editTextIzvornaValuta = findViewById<EditText>(R.id.editTextIzvornaValuta)
        editTextIzvornaValuta.setText("1") //default vrijednost kako polje nebi bilo prazno

        val editTextZeljenaValuta = findViewById<EditText>(R.id.editTextZeljenaValuta)
        editTextZeljenaValuta.isEnabled = false //zelimo da nam IV i ZV izgledaju jednako, ali ne i da nam korisnik unese broj u ZV

        //IV_spinnerValue i ZV_spinnerValue bi trebali biti pohranjeni u bazi kao korisnicke postavke, ali nisam nasao vremena da to napravim
        var IV_spinnerValue = 50 //izvorna valuta default vrijednost (EUR) i holder buduce vrijednosti
        var ZV_spinnerValue = 155 //zeljena valuta default vrijednost (USD) i holder buduce vrijednosti

        var exchange_rate = 0.0
        var autoIzracunajValue = true //switch koji omogućuje i onemogućuje automatski izračun

        //array svih valuta
        val currenciesArray = resources.getStringArray(R.array.currencies_array) //preuzmi array iz strings.xml
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, currenciesArray
        ) // za spojiti data source i Spinner view


        //--------------------------------------------------------<API call za exchange_rate i pohrana u bazu podataka>------------------------------------------------------------------------//

        //API call mora biti unutar lifecycleScope-a kako nebi zablokirao main thread (jer je izgledno da će trebati neko vrijeme da se izvrši (ali onda opet korisnik nema tu informaciju)
        //izbaceno jer nikako nije htjelo raditi, a i korisniku je ta informacija nužna za daljnje akcije, pa ima smisla da nije async
        /*lifecycleScope.launch {
            val exchange_rate = withContext(Dispatchers.IO) {
                getExchangeRate(from_currency = currenciesArray[IV_spinnerValue],
                    to_currency = currenciesArray[ZV_spinnerValue],
                    context = applicationContext)
            }*/
            exchange_rate = getExchangeRate(from_currency=currenciesArray[IV_spinnerValue], to_currency=currenciesArray[ZV_spinnerValue], context= applicationContext)
            //Log.d("getExchangeRate Root", "Za $currenciesArray[IV_spinnerValue] u $currenciesArray[ZV_spinnerValue] omjer je $exchange_rate")
        //}
        //--------------------------------------------------------</API call za exchange_rate i pohrana u bazu podataka>------------------------------------------------------------------------//


        //--------------------------------------------------------spinnerIzvornaValuta------------------------------------------------------------------------//

        spinnerIzvornaValuta.adapter = adapter
        spinnerIzvornaValuta.setSelection(IV_spinnerValue)

        spinnerIzvornaValuta.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (view == null){ return } // return if the view is null

                //if(IV_spinnerValue != position){ //nije potrebno jer android po default-u nece pozvati funkciju za dva puta odabranu istu stvar

                    //Log.d("IV_spinnerValue Change", "IV_spinnerValue($IV_spinnerValue) = position($position)")
                    IV_spinnerValue = position
                    /*lifecycleScope.launch {
                        exchange_rate = withContext(Dispatchers.IO) {
                            getExchangeRate(from_currency = currenciesArray[IV_spinnerValue],
                                to_currency = currenciesArray[ZV_spinnerValue],
                                context = applicationContext)
                        }*/
                        exchange_rate = getExchangeRate(from_currency=currenciesArray[IV_spinnerValue], to_currency=currenciesArray[ZV_spinnerValue], context= applicationContext)
                        //nije pametno da ovo bude suspended fun jer korisnik odmah zeli napraviti kalkulaciju s tom brojkom, ali ne znam kako zaobici android error koji tada dobijem
                        //procjena je da vrijeme trajanja API call+obrada returna+zapisivanje u bazu nebi trebalo trajati vise od 500ms sto korisnik nebi trebao osjetiti
                        //Log.d("IV_spinnerValue", "Za "+currenciesArray[IV_spinnerValue]+" u "+ currenciesArray[ZV_spinnerValue] + " omjer je " + exchange_rate)

                        buttonIzracunaj.performClick()
                    //}

                //}
            }



            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        //--------------------------------------------------------spinnerZeljenaValuta------------------------------------------------------------------------//
        spinnerZeljenaValuta.adapter = adapter
        spinnerZeljenaValuta.setSelection(ZV_spinnerValue)

        spinnerZeljenaValuta.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (view == null) return // return if the view is null

                //if(ZV_spinnerValue != position){ //ako je odabrana drugacija valuta, napravimo update spinner-a i povucemo iz baze conversion rate za odabrani IV i ZV par
                //Log.d("IV_spinnerValue Change", "IV_spinnerValue($IV_spinnerValue) = position($position)")
                ZV_spinnerValue = position

                    /*lifecycleScope.launch {
                        exchange_rate = withContext(Dispatchers.IO) {
                            getExchangeRate(from_currency = currenciesArray[IV_spinnerValue],
                                to_currency = currenciesArray[ZV_spinnerValue],
                                context = applicationContext)
                        }*/
                        exchange_rate = getExchangeRate(from_currency=currenciesArray[IV_spinnerValue], to_currency=currenciesArray[ZV_spinnerValue], context= applicationContext)
                        //nije pametno da ovo bude suspended fun jer korisnik odmah zeli napraviti kalkulaciju s tom brojkom, ali ne znam kako zaobici android error koji tada dobijem
                        //procjena je da vrijeme trajanja API call+obrada returna+zapisivanje u bazu nebi trebalo trajati vise od 500ms sto korisnik nebi trebao osjetiti
                        //Log.d("ZV_spinnerValue", "Za "+currenciesArray[IV_spinnerValue]+" u "+ currenciesArray[ZV_spinnerValue] + " omjer je " + exchange_rate)
                        buttonIzracunaj.performClick()
                    //}
                //}
            }

            override fun onNothingSelected(parent: AdapterView<*>) { /*do nothing*/ }
        }

        //--------------------------------------------------------autoIzracunaj------------------------------------------------------------------------//
        autoIzracunaj.isChecked = autoIzracunajValue //postavljanje default vrijednost
        autoIzracunaj.setOnCheckedChangeListener { button, autoIzracunajChange ->
            autoIzracunajValue = autoIzracunajChange //direktno stavljanje autoIzracunajValue na mjesto Change-a nebi radilo jer je Change lokalni val

            buttonIzracunaj.performClick() //dobro je i sami izracun odmah pozvati jer korisnik to vjerovatno ocekuje
        }



        //--------------------------------------------------------editTextIzvornaValuta------------------------------------------------------------------------//
        //prilikom svakog edita izvorne value se pokrece racunica tako da je gumb za racunanje zapravo samo za to da ga se "virtualno" klikne
        editTextIzvornaValuta.doOnTextChanged { text, start, count, after ->
            if(autoIzracunajValue) { //ako je "Auto izračunaj" true, omogući da se poziva gumb racunanja prilikom promjene polja
                buttonIzracunaj.performClick()
            }
        }



        //--------------------------------------------------------IV_default------------------------------------------------------------------------//
        IV_default_10.setOnClickListener{ editTextIzvornaValuta.setText("10"); buttonIzracunaj.performClick(); }
        IV_default_20.setOnClickListener{ editTextIzvornaValuta.setText("20"); buttonIzracunaj.performClick(); }
        IV_default_30.setOnClickListener{ editTextIzvornaValuta.setText("30"); buttonIzracunaj.performClick(); }
        IV_default_50.setOnClickListener{ editTextIzvornaValuta.setText("50"); buttonIzracunaj.performClick(); }
        IV_default_100.setOnClickListener{ editTextIzvornaValuta.setText("100"); buttonIzracunaj.performClick(); }



        //--------------------------------------------------------buttonZamjeniValute------------------------------------------------------------------------//
        buttonZamjeniValute.setOnClickListener {
            spinnerIzvornaValuta.setSelection(ZV_spinnerValue)
            spinnerZeljenaValuta.setSelection(IV_spinnerValue)

            //ovo sve skupa je samo zajena vrijednosti izmedu IV i ZV, ali bez pomocne varijable
            IV_spinnerValue = IV_spinnerValue + ZV_spinnerValue // a = a + b
            ZV_spinnerValue = IV_spinnerValue - ZV_spinnerValue // b = a - b
            IV_spinnerValue = IV_spinnerValue - ZV_spinnerValue // a = a - b

            buttonIzracunaj.performClick() //napravi kao da je korisnik kliknuo gumb izracunaj
                //ovo radimo da od 1 EUR = 1.1 USD (tocno) nebi dobili 1 USD = 1.1 EUR (krivo), odnosno da se nebi samo valute zamjenile, a vrijednosti ostale na istim mjestima
        }



        //--------------------------------------------------------buttonIzracunaj------------------------------------------------------------------------//
        buttonIzracunaj.setOnClickListener {

            try {
                var IV_editTextValue = editTextIzvornaValuta.text.toString().toDouble()

                if(IV_editTextValue == null){ //ako je vrijednost prazna da ne baci gresku
                    return@setOnClickListener
                }

                //IV_spinnerValue je trenutna vrijednost izvorne valute
                //ZV_spinnerValue je trenutna vrijednost izvorne valute

                ///// Ovo je bilo prvotno rjesenje dok nije implmentirana SQL baza i API call
                //Uzeto na dan 01.05.2023. 22:11 CET prema google.com (npr. 1 USD to EUR)
                //matrica kojoj se predaju ZV i IV vrijednosti, te se dobije konverzijski tečaj
                /*val conversionRates:Array<Array<Double>> = arrayOf(
                    //IZVOR:AUD     CHF     CNY     EUR     USD    ZELJENA:
                    arrayOf(1.0,    1.68,   0.22,   1.66,   1.51), // AUD
                    arrayOf(0.59,   1.0,    0.13,   0.98,   0.90), // CHF
                    arrayOf(4.58,   7.71,   1.0,    7.62,   6.91), // CNY
                    arrayOf(0.60,   1.02,   0.13,   1.0,    0.91), // EUR
                    arrayOf(0.66,   1.12,   0.14,   1.10,   1.0)  // USD
                )*/

                //var zeljenaValutaCalc = String.format("%.3f", IV_editTextValue * conversionRates[ZV_spinnerValue][IV_spinnerValue])
                    // unesenu IV vrijednost množimo s konverzijskom vrijednosti iz prethodne matrice
                    // dobeiveni broj moramo srezati na par decimala jer će se inače prikazati (ne)preciznost float-a


                var zeljenaValutaCalc = String.format("%.5f", IV_editTextValue * exchange_rate)

                editTextZeljenaValuta.setText(zeljenaValutaCalc)
            } catch (e: Exception) {
                showToast(message = "Unesena vrijednost nije ispravna")
                //return@setOnClickListener
            }
        }



        //--------------------------------------------------------buttonObrisiSadrzaj------------------------------------------------------------------------//
        buttonObrisiSadrzaj.setOnClickListener {
            editTextIzvornaValuta.setText("")
            editTextZeljenaValuta.setText("")
        }
    }


    // override the onOptionsItemSelected(), function to implement, the item click listener callback, to open and close the navigation, drawer when the icon is clicked
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }


    /*
    override fun onStart(savedInstanceState: Bundle?) {
        showToast(message = "Dobrodošli!") //implementirano samo kako bi se iskoristio Activity Lifecycle
    }

    override fun onResume(savedInstanceState: Bundle?) {
        showToast(message = "Dobrodošli natrag!") //implementirano samo kako bi se iskoristio Activity Lifecycle
    }

    override fun onPause(savedInstanceState: Bundle?) {

    }

    override fun onStop(savedInstanceState: Bundle?) {
        showToast(message = "Doviđenja!") //implementirano samo kako bi se iskoristio Activity Lifecycle
    }
       */

    override fun onDestroy() {
        super.onDestroy()

        try{
            dbHelper.close() // zatvori dbHelper kada se gasi aplikacija
        }catch(exp: Exception){
            //don't care
        }

    }
}

class MyDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "currencyConversionTableN"
        private const val DATABASE_VERSION = 2
    }

    override fun onCreate(db: SQLiteDatabase?) {
        Log.d("Database", "onCreate()")
        db?.execSQL("DROP TABLE IF EXISTS currencyConversionTableN;") //TODO ugasiti kasnije, samo za testiranje
        db?.execSQL("""
                CREATE TABLE IF NOT EXISTS currencyConversionTableN (
                    from_currency TEXT NOT NULL,
                    to_currency TEXT NOT NULL,
                    exchange_rate REAL,
                    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (from_currency, to_currency)
                );
            """)

        Log.d("Database", "Database created")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //onCreate(db)
        Log.d("Database", "onUpgrade()")
    }

    //#############accesing database via terminal#################//
    //  adb -s emulator-5554 shell
    //  su
    //  sqlite3 data/data/com.example.v3/databases/currencyConversionTableN
    //  SELECT * FROM currencyConversionTableN;
}

fun getExchangeRate(from_currency: String, to_currency: String, context: Context): Double{
    var dbHelper = MyDatabaseHelper(context)
    var dbWrite = dbHelper.writableDatabase
    var cursor = dbWrite.rawQuery("SELECT exchange_rate, last_update FROM currencyConversionTableN WHERE from_currency='$from_currency' AND to_currency='$to_currency';", null)

    var exchangeRate = 0.0
    if (cursor.moveToFirst()) {
        exchangeRate = cursor.getDouble(0)
        val lastUpdate = cursor.getString(1) // 2023-06-03 15:12:03

        cursor.close()
        dbHelper.close()

        if(!isTimestampOlderThan24Hours(lastUpdate)){ // ako exchange_rate iz baze nije stariji od 24h, ne treba ga osvjeziti
            Log.d("getExchangeRate-baza", "exchange rate iz baze je $exchangeRate")
            return exchangeRate // returna exchange_rate iz baze
        }

        Log.d("getExchangeRate-baza", "exchange rate iz baze je stariji od 24h (lastUpdate), treba ga osvjeziti")
    }

    //ako je kursor prazan ili podatak stariji od 24h, trazeni exchange_rate nije pronaden u bazi, pa ga treba ici dohvatiti kroz API (koji onda takoder napuni bazu)
    val exchangeRateApi = getLatestCurrencyConversionRates(from_currency, to_currency, context, dbHelper)
    if(exchangeRateApi != 0.0){
        Log.d("getExchangeRate-API", "Nema/staro u bazi! API call -> $from_currency u $to_currency = $exchangeRateApi")
        return exchangeRateApi // returna exchange_rate iz baze ili API-a
    }


    Log.d("ERROR:getExchangeRate", "Nema u bazi, API call nije vratio omjer")
    return exchangeRate // ako nije pronaden u bazi ili je bio star, a ni API nije vratio omjer, vrati default 0.0 (ili možda starog iz baze)

}

fun isTimestampOlderThan24Hours(timestamp: String): Boolean {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val parsedTimestamp = LocalDateTime.parse(timestamp, formatter)
    val currentTimestamp = LocalDateTime.now()

    val hoursDifference = java.time.Duration.between(parsedTimestamp, currentTimestamp).toHours()
    return hoursDifference > 24
}

fun getLatestCurrencyConversionRates(baseCurrency: String, to_currency: String, context: Context, dbHelper: MyDatabaseHelper): Double{
    // kroz ovu funkciju se dohvacaju sve valute u koju se to_currency mogao pretvoriti jer tako radit API s kojeg se dobivaju iste
    // moglo ga se limitirati na samo trazenog, no ovako ce se koristiti manje API call-ova
    // ali svejedno je return exchange_rate izmedu trazenih

    val apiKey = "0AD0hc9VsupNA5Vp4mWBMAzjHjQjWA9oX554du9p" //rucno generiran, 300 zahtjeva mjesecno

    //zaobilazenje android policy-a da na main thread-u ne mogu biti network taskovim kako nebi morali ici async (jer korisnik informaciju treba odmah)
    val policy = ThreadPolicy.Builder().permitAll().build()
    StrictMode.setThreadPolicy(policy)

    try {
        var apiJSONresponse: String = ""

        val url = URL("https://api.currencyapi.com/v3/latest?apikey=$apiKey&base_currency=$baseCurrency")
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            apiJSONresponse = connection.inputStream.bufferedReader().use { it.readText() }
            //Log.d("getLatestCurrencyConversionRates",  "$responseCode --> apiJSONresponse je $apiJSONresponse")
            connection.disconnect()
        } else {
            connection.disconnect()
            0.0 //return failure
        }

        //for testing to avoid going over API call limit (same thing that API returns, just shortened, but added EUR->USD to end)
        //apiJSONresponse = "{\"meta\":{\"last_updated_at\":\"2023-05-21T23:59:59Z\"},\"data\":{\"ADA\":{\"code\":\"ADA\",\"value\":3.002739},\"AED\":{\"code\":\"AED\",\"value\":3.974141},\"AFN\":{\"code\":\"AFN\",\"value\":94.520366},\"ALL\":{\"code\":\"ALL\",\"value\":111.688548},\"AMD\":{\"code\":\"AMD\",\"value\":418.198986},\"ANG\":{\"code\":\"ANG\",\"value\":1.945201},\"AOA\":{\"code\":\"AOA\",\"value\":584.95038},\"ARS\":{\"code\":\"ARS\",\"value\":250.586764},\"AUD\":{\"code\":\"AUD\",\"value\":1.625228},\"AVAX\":{\"code\":\"AVAX\",\"value\":0.075588},\"AWG\":{\"code\":\"AWG\",\"value\":1.950195},\"AZN\":{\"code\":\"AZN\",\"value\":1.839808},\"BAM\":{\"code\":\"BAM\",\"value\":1.955599},\"BBD\":{\"code\":\"BBD\",\"value\":2.185354},\"BDT\":{\"code\":\"BDT\",\"value\":115.828114},\"BGN\":{\"code\":\"BGN\",\"value\":1.958859},\"BHD\":{\"code\":\"BHD\",\"value\":0.407258},\"BIF\":{\"code\":\"BIF\",\"value\":3043.487878},\"BMD\":{\"code\":\"BMD\",\"value\":1.082239},\"BNB\":{\"code\":\"BNB\",\"value\":0.003527},\"BND\":{\"code\":\"BND\",\"value\":1.45699},\"BOB\":{\"code\":\"BOB\",\"value\":7.458237},\"BRL\":{\"code\":\"BRL\",\"value\":5.409356},\"BSD\":{\"code\":\"BSD\",\"value\":1.07939},\"BTC\":{\"code\":\"BTC\",\"value\":4.0e-5},\"BTN\":{\"code\":\"BTN\",\"value\":89.225096},\"BWP\":{\"code\":\"BWP\",\"value\":14.704499},\"BYN\":{\"code\":\"BYN\",\"value\":2.731895},\"BYR\":{\"code\":\"BYR\",\"value\":21211.85875},\"BZD\":{\"code\":\"BZD\",\"value\":2.175578},\"CAD\":{\"code\":\"CAD\",\"value\":1.459995},\"CDF\":{\"code\":\"CDF\",\"value\":2494.56222},\"CHF\":{\"code\":\"CHF\",\"value\":0.972154},\"CLF\":{\"code\":\"CLF\",\"value\":0.031326},\"CLP\":{\"code\":\"CLP\",\"value\":864.373781},\"CNY\":{\"code\":\"CNY\",\"value\":7.584222},\"COP\":{\"code\":\"COP\",\"value\":4915.532575},\"CRC\":{\"code\":\"CRC\",\"value\":576.740769},\"CUC\":{\"code\":\"CUC\",\"value\":1.0876},\"CUP\":{\"code\":\"CUP\",\"value\":25.904353},\"CVE\":{\"code\":\"CVE\",\"value\":110.983623},\"CZK\":{\"code\":\"CZK\",\"value\":23.771396},\"DJF\":{\"code\":\"DJF\",\"value\":192.335599},\"DKK\":{\"code\":\"DKK\",\"value\":7.448579},\"DOP\":{\"code\":\"DOP\",\"value\":58.92397},\"DOT\":{\"code\":\"DOT\",\"value\":0.204695},\"DZD\":{\"code\":\"DZD\",\"value\":147.568674},\"EGP\":{\"code\":\"EGP\",\"value\":33.351573},\"ERN\":{\"code\":\"ERN\",\"value\":16.233594},\"ETB\":{\"code\":\"ETB\",\"value\":58.908966},\"ETH\":{\"code\":\"ETH\",\"value\":0.000596},\"EUR\":{\"code\":\"EUR\",\"value\":1},\"FJD\":{\"code\":\"FJD\",\"value\":2.399215},\"FKP\":{\"code\":\"FKP\",\"value\":0.868728},\"GBP\":{\"code\":\"GBP\",\"value\":0.868529},\"GEL\":{\"code\":\"GEL\",\"value\":2.748887},\"GGP\":{\"code\":\"GGP\",\"value\":0.868728},\"GHS\":{\"code\":\"GHS\",\"value\":11.688184},\"GIP\":{\"code\":\"GIP\",\"value\":0.868729},\"GMD\":{\"code\":\"GMD\",\"value\":64.555582},\"GNF\":{\"code\":\"GNF\",\"value\":9361.365685},\"GTQ\":{\"code\":\"GTQ\",\"value\":8.424141},\"GYD\":{\"code\":\"GYD\",\"value\":228.276643},\"HKD\":{\"code\":\"HKD\",\"value\":8.459702},\"HNL\":{\"code\":\"HNL\",\"value\":26.52929},\"HRK\":{\"code\":\"HRK\",\"value\":7.5345},\"HTG\":{\"code\":\"HTG\",\"value\":154.773151},\"HUF\":{\"code\":\"HUF\",\"value\":375.715655},\"IDR\":{\"code\":\"IDR\",\"value\":16152.422046},\"ILS\":{\"code\":\"ILS\",\"value\":3.950822},\"IMP\":{\"code\":\"IMP\",\"value\":0.868728},\"INR\":{\"code\":\"INR\",\"value\":89.723065},\"IQD\":{\"code\":\"IQD\",\"value\":1413.955151},\"IRR\":{\"code\":\"IRR\",\"value\":45724.627141},\"ISK\":{\"code\":\"ISK\",\"value\":151.307878},\"JEP\":{\"code\":\"JEP\",\"value\":0.868728},\"JMD\":{\"code\":\"JMD\",\"value\":166.722916},\"JOD\":{\"code\":\"JOD\",\"value\":0.767849},\"JPY\":{\"code\":\"JPY\",\"value\":149.019425},\"KES\":{\"code\":\"KES\",\"value\":148.807953},\"KGS\":{\"code\":\"KGS\",\"value\":94.717611},\"KHR\":{\"code\":\"KHR\",\"value\":4436.546863},\"KMF\":{\"code\":\"KMF\",\"value\":492.959881},\"KPW\":{\"code\":\"KPW\",\"value\":973.959366},\"KRW\":{\"code\":\"KRW\",\"value\":1434.193926},\"KWD\":{\"code\":\"KWD\",\"value\":0.33241},\"KYD\":{\"code\":\"KYD\",\"value\":0.899508},\"KZT\":{\"code\":\"KZT\",\"value\":485.270932},\"LAK\":{\"code\":\"LAK\",\"value\":19016.061646},\"LBP\":{\"code\":\"LBP\",\"value\":16200.839748},\"LKR\":{\"code\":\"LKR\",\"value\":331.375972},\"LRD\":{\"code\":\"LRD\",\"value\":181.220962},\"LSL\":{\"code\":\"LSL\",\"value\":21.049552},\"LTC\":{\"code\":\"LTC\",\"value\":0.011701},\"LTL\":{\"code\":\"LTL\",\"value\":3.195567},\"LVL\":{\"code\":\"LVL\",\"value\":0.654635},\"LYD\":{\"code\":\"LYD\",\"value\":5.17847},\"MAD\":{\"code\":\"MAD\",\"value\":10.947585},\"MATIC\":{\"code\":\"MATIC\",\"value\":1.263153},\"MDL\":{\"code\":\"MDL\",\"value\":19.142045},\"MGA\":{\"code\":\"MGA\",\"value\":4749.515957},\"MKD\":{\"code\":\"MKD\",\"value\":61.2},\"USD\":{\"code\":\"USD\",\"value\":1.12}}}"

        // Parse the JSON response
        val jsonObject = JSONObject(apiJSONresponse)
        val dataObject = jsonObject.getJSONObject("data")

        var query = ""
        var requestedExchangeRate = 0.0

        //var dbHelper = MyDatabaseHelper(context) //baza podataka //koristi se inicijalizacija db helper-a iz getExchangeRate() funkcije
        val dbWrite = dbHelper.writableDatabase // Perform database operations

        for (toCurrencyKey in dataObject.keys()) { // petlja kroz sve key-value pair kako bi generirali SQL query za svakoga

            val exchange_rateValue = dataObject.getJSONObject(toCurrencyKey).getDouble("value") // Get the conversion rate value from the JSON response

            //ne mozemo biti sigurni da li vec from&to kombinacija postoji u bazi pa idemo s malo pametnijim query-em
            // nad from_currency i to_currency imamo composite primary key, pa samim time INSERT nece raditi ako vec postoji ta kombinacija
            // ali ce zato UPDATE nad tim redom promjeniti exchange_rate
            /*query += """INSERT INTO currencyConversionTableN (from_currency, to_currency, exchange_rate)
                    VALUES ('$baseCurrency', '$toCurrencyKey', $exchange_rateValue) 
                    ON DUPLICATE KEY UPDATE conversion_rate=$exchange_rateValue; """ */ //ovo ne radi za SQLlite jer nema ON DUPLICATE KEY

            query = """INSERT OR REPLACE INTO currencyConversionTableN (from_currency, to_currency, exchange_rate)
                        VALUES ('$baseCurrency', '$toCurrencyKey', $exchange_rateValue);""" //solution for previous problem

            dbWrite.execSQL(query) //izvrsavanje jednog po jednog query-a, ako ima problema baciti ce exception pa samim time skace odmah dolje
                // bolje bi bilo da smo to sve mogli sakupiti u jedan query, no sqlite nema opciju izvršavanja više od jednog query-a

            //provjerimo da li je to prvotno trazeni
            if(toCurrencyKey == to_currency && !exchange_rateValue.isNaN()){ //trazeni jer samo se toCurrency treba poklopiti obzirom da dobijemo samo njih u odgovoru koji je za from_currency
                requestedExchangeRate = exchange_rateValue
            }

        }

        dbWrite.close() // Close the database connection
        dbHelper.close() // Close the database helper
        return requestedExchangeRate //return

    } catch (e: Exception) {
        Log.d("getLatestCurrency-error:", e.toString())
        return 0.0 //return failure
    }
}