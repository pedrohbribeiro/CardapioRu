package cf.pedroribeiro.cardapioru;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.goebl.david.Webb;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    Map<String,String> ru;
    final String bgColor = "#FAFAFA";
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((WebView)findViewById(R.id.webView)).loadDataWithBaseURL(null,"<body bgcolor=\""+bgColor+"\">" + "Carregando" + "</body>","text/html","utf-8",null);

        ru = new HashMap<>();
        ru.put("RU Setorial I","6");
        ru.put("RU Setorial II","1");
        ru.put("RU Saúde e Direito","2");
        ru.put("RU ICA","5");

        SharedPreferences prefs = getSharedPreferences("cardapioPrefs",MODE_PRIVATE);
        String ruSelecionado = prefs.getString("ru", "RU Setorial I");

        Spinner spinner = findViewById(R.id.spinnerRestaurantes);
        String[] arraySpinner = new String[] {"RU Setorial I", "RU Setorial II", "RU Saúde e Direito", "RU ICA"}; //6,1,2,5
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getPosition(ruSelecionado));
        spinner.setOnItemSelectedListener(this);

        httpRequest();
    }

    private void httpRequest() {
        new Thread() {
            @Override
            public void run() {
                String response;
                try {

                    SharedPreferences prefs = getSharedPreferences("cardapioPrefs",MODE_PRIVATE);
                    String ruSelecionado = prefs.getString("ru", "RU Setorial I");

                    String url = "http://www.fump.ufmg.br/cardapio.aspx";
                    Webb webb = Webb.create();//
                    String response1 = webb.get(url).ensureSuccess().asString().getBody();

                    Document doc = Jsoup.parse(response1);
                    String viewState = doc.getElementById("__VIEWSTATE").val();
                    String viewStateGenerator = doc.getElementById("__VIEWSTATEGENERATOR").val();
                    String eventValidation = doc.getElementById("__EVENTVALIDATION").val();
                    String eventTarget = "ctl00$contentPlaceHolder$drpRestaurante";
                    String eventArgument = "";
                    String ct100txtBusca = "api";
                    String ct100contentPlaceHolderdrpRestaurante = ru.get(ruSelecionado);

                    response = webb.post(url)
                            .param("__EVENTTARGET", eventTarget)
                            .param("__EVENTARGUMENT", eventArgument)
                            .param("__LASTFOCUS", "")
                            .param("__VIEWSTATE", viewState)
                            .param("__VIEWSTATEGENERATOR", viewStateGenerator)
                            .param("__EVENTVALIDATION", eventValidation)
                            .param("ctl00$txtBusca", ct100txtBusca)
                            .param("ctl00$contentPlaceHolder$drpRestaurante", ct100contentPlaceHolderdrpRestaurante) //Número do RU
                            .ensureSuccess()
                            .asString()
                            .getBody();
                    if(response.contains("Não existe cardápio cadastrado para a data selecionada.") || response.contains("Não há funcionamento do restaurante universitário neste dia")){
                        response = "Não existe cardápio cadastrado para hoje.";
                    } else {
                        doc = Jsoup.parse(response);
                        response =  doc.getElementById("carte").toString();
                    }
                } catch (
                        Exception ex) {
                   response = (ex.toString());
                   if (response.contains("java.net.UnknownHostException")){
                       response = "Não foi possível se conectar ao site";
                   }
                }
                final String finalResponse = "<body bgcolor=\""+bgColor+"\">" + response + "</body>";
                runOnUiThread(() -> {
                    ((WebView)findViewById(R.id.webView)).loadDataWithBaseURL(null,finalResponse,"text/html","utf-8",null);
                });

            }
        }.start();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String ruSelecionado = parent.getItemAtPosition(position).toString();
        SharedPreferences prefs = getSharedPreferences("cardapioPrefs",MODE_PRIVATE);
        prefs.edit().putString("ru",ruSelecionado).apply();
        ((WebView)findViewById(R.id.webView)).loadDataWithBaseURL(null,"<body bgcolor=\""+bgColor+"\">" + "Carregando" + "</body>","text/html","utf-8",null);
        httpRequest();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
