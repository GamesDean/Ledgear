package com.menowattge.myled;

import android.app.Fragment;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import es.dmoral.toasty.Toasty;
import ir.sohreco.circularpulsingbutton.CircularPulsingButton;
import mbanje.kurt.fabbutton.FabButton;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CharacteristicFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CharacteristicFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CharacteristicFragment extends Fragment {

    // Factory generated
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // Factory generated
    private String mParam1;
    private String mParam2;

    protected  CircularPulsingButton pulseButtonConnect;
    protected  FabButton fabButtonDisconnect;

    private OnFragmentInteractionListener mListener;
    protected Spinner spinnerDue ;
    protected Spinner spinnerPower ;

    protected ArrayAdapter adapter;
    protected ArrayAdapter adapterPower;
    protected  FabButton invia;
    protected TextView textView;
    protected TextView textViewFive;
   /// protected ImageView imageDiagram;
   /// protected ImageButton buttonClose;

   /// protected RelativeLayout layoutDiagram;

    protected RelativeLayout fl;
    protected FrameLayout mrT;
    protected TextView time ;

    protected String selezionaProgramma = ">seleziona un programma<";
    protected String programmaUno = "Programma 1:23M2";
    protected String programmaDue ="Programma 2:23M3";
    protected String programmaTre = "Programma 3:22M2";
    protected String programmaQuattro="Programma 4:22M3";
    protected String programmaCinque="Programma 5:ERP";
    protected String programmaSei="Programma 6:EMX";
    protected String programmaSette="Programma 7:23EMP";
    protected String programmaOtto="Programma 8:22EMP";
    protected String programmaNove="Programma 9:23ERP";
    protected String programmaDieci="Programma 10:22ERP";
    protected String programmaUndici="Programma 11:23M2S2";
    protected String programmaDodici="Programma 12:23M3S2";
    protected String programmaTredici="Programma 13:22M2S2";
    protected String programmaQuattordici="Programma 14:22M3S2";
    protected String programmaQuindici="Programma 15:LSM2";
    protected String programmaSedici="Programma 16:LSM3";
    protected String programmaDiciassette="Programma 17:LSM2S2";
    protected String programmaDiciotto="Programma 18:LSM3S2";
    protected String programmaDiciannove="Programma 19:R400";
    protected String programmaVenti="Programma 20:22DMP";

    protected  String selezionaPotenza = ">corrente max<";
    protected  String potenzaQuattrocento ="400[mA]";
    protected  String potenzaCinquecento = "500[mA]";
    protected  String potenzaCinqueCinquanta ="550[mA]";
    protected  String potenzaSeiCinquanta ="650[mA]";
    protected  String potenzaSettecento ="700[mA]";

    protected  int hour=0;

    protected View view = null;

    public CharacteristicFragment() {
        // Required empty public constructor
    }

    /**
     * Usually called from MainActivity inside the listener of the Connect Button, once the connection is established it shows the UUID and some suggestions
     * Then the spinner fade in.
     * @param a
     */
    public void showUuid(UUID a){
        if(a != null) {
            Toasty.success(getActivity(),"connesso al Lemset",Toast.LENGTH_SHORT,true).show();
            Toasty.info(getActivity(), "seleziona un programma", Toast.LENGTH_SHORT).show();

            final Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(spinnerDue.getContext(), android.R.anim.fade_in);
            final Animation fadeOut = android.view.animation.AnimationUtils.loadAnimation(spinnerDue.getContext(), android.R.anim.fade_out);

            fl = (RelativeLayout) view.findViewById(R.id.charactRelativeLayout);
            mrT = (FrameLayout) view.findViewById(R.id.charactFrameLayout);



            // background disappear
            fl.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fl.setAnimation(fadeOut);
                    fl.setVisibility(View.INVISIBLE);
                }
            }, 2500);//2500


            // background appear
            fl.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fl.setBackgroundResource(R.mipmap.ge_frag_tel_w);
                    fl.setAnimation(fadeIn);
                    fl.setVisibility(View.VISIBLE);

                    time.setVisibility(View.VISIBLE);
                }
            }, 3000);//3000


            // spinner appear
            spinnerDue.postDelayed(new Runnable() {
                @Override
                public void run() {

                    // ad avvenuta connessione spinner fade in
                    spinnerDue.setAnimation(fadeIn);
                    spinnerDue.setVisibility(View.VISIBLE);

                }
            }, 3300);//3300

        }else {
            Toasty.error(getActivity(), "connessione non avvenuta : premi exit e riprova", Toast.LENGTH_SHORT).show();

        }

    }



    /**ƒ
     *
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CharacteristicFragment.
     *
     */
    public static CharacteristicFragment newInstance(String param1, String param2) {
        CharacteristicFragment fragment = new CharacteristicFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }




    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {


        view = inflater.inflate(R.layout.fragment_characteristic, container, false);

        // ora nel telefono mostrato nel background
        time = (TextView)view.findViewById(R.id.textView4);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        if (calendar!=null) {
            hour = calendar.get(Calendar.HOUR_OF_DAY);
        }
        else {
            hour = 10;
        }
        time.setText(""+hour+":");
        time.setVisibility(view.INVISIBLE);

        invia = (FabButton)view.findViewById(R.id.buttonInvia);

        // testo sopra i pulsanti "connect" e"disconnect", obbligatorio dato che i FAB non hanno il testo
        textView =(TextView)view.findViewById(R.id.textView2);
        textViewFive =(TextView)view.findViewById(R.id.textView5);

      ///  imageDiagram=(ImageView)view.findViewById(R.id.imageDiagram);
      /// layoutDiagram=(RelativeLayout)view.findViewById(R.id.layoutDiagram);
       /// buttonClose=(ImageButton)view.findViewById(R.id.imageButton);

        pulseButtonConnect=(CircularPulsingButton) view.findViewById(R.id.pulsebuttonConnect);
        fabButtonDisconnect=(FabButton)view.findViewById(R.id.fabButtonDisconnect);
        fabButtonDisconnect.setVisibility(View.INVISIBLE);

        spinnerDue = (Spinner)view.findViewById(R.id.spinner2);
        spinnerPower = (Spinner)view.findViewById(R.id.spinnerPower);

///        layoutDiagram.setVisibility(View.INVISIBLE);


        // adapter che va a riempire lo spinner contenente il  programma
        adapter = new ArrayAdapter<>(getActivity(),R.layout.spinner_text,
                new String[]{selezionaProgramma, //hint
                        programmaUno,programmaDue,programmaTre,programmaQuattro,programmaCinque,programmaSei,
                        programmaSette, programmaOtto,programmaNove,programmaDieci,programmaUndici,programmaDodici,
                        programmaTredici, programmaQuattordici,programmaQuindici,programmaSedici,programmaDiciassette,
                        programmaDiciotto,programmaDiciannove,programmaVenti});

        spinnerDue.setAdapter(adapter);
        
        // adapter che va a riempire lo spinner contenente la potenza
        adapterPower = new ArrayAdapter<>(getActivity(),R.layout.spinner_text,
                new String[]{selezionaPotenza, // hint
                        potenzaQuattrocento,potenzaCinquecento,potenzaCinqueCinquanta,potenzaSeiCinquanta,potenzaSettecento});

        spinnerPower.setAdapter(adapterPower);

        // li rendo invisibili per poi farli riapparire con un'animazione
        spinnerDue.setVisibility(View.INVISIBLE);
        spinnerPower.setVisibility(View.INVISIBLE);
        invia.setVisibility(View.INVISIBLE);
        textView.setVisibility(View.INVISIBLE);
        textViewFive.setVisibility(View.INVISIBLE);


        pulseButtonConnect.setEnabled(false);
        pulseButtonConnect.setActivated(false);


        // connetto
        pulseButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity) getActivity()).connectToDevice(((MainActivity) getActivity()).deviceAddress);
                final Animation animDisLeft = android.view.animation.AnimationUtils.loadAnimation(fabButtonDisconnect.getContext(),  android.R.anim.slide_in_left);
                final Animation animCon = android.view.animation.AnimationUtils.loadAnimation(fabButtonDisconnect.getContext(),  R.anim.scale_down);

                // connect disappear
                pulseButtonConnect.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pulseButtonConnect.setAnimation(animCon);
                        pulseButtonConnect.setVisibility(View.INVISIBLE);

                    }
                },300);



                // disconnect appear
                fabButtonDisconnect.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fabButtonDisconnect.setAnimation(animDisLeft);
                        fabButtonDisconnect.setVisibility(View.VISIBLE);

                        textViewFive.setAnimation(animDisLeft);
                        textViewFive.setVisibility(View.VISIBLE);
                    }
                },3500);

            }
        });

        // disconnetto
        fabButtonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                BluetoothGatt btGatt = ((MainActivity) getActivity()).connectToDevice(((MainActivity) getActivity()).deviceAddress);

                ((MainActivity) getActivity()).discConnectToDevice(btGatt);
                //rimuove il fragment dallo stack come quando si preme il tasto "back"
                getFragmentManager().popBackStack();
                //deve tornare invisibile
                MainActivity.startScanningButton.setVisibility(View.INVISIBLE);
                //reimposto il valore a zero in modo tale che ricompaia il pulsante dell'energia/potenza
                ((MainActivity) getActivity()).x = 0;
                //reimposto il valore a zero in modo tale che ricompaia il pulsante "ok"
                ((MainActivity) getActivity()).j = 0;
                //deve tornare cliccabile
                MainActivity.startScanningButton.setEnabled(true);
                //riesegue la scansione per ripopolare la lista
                ((MainActivity)getActivity()).startScanning();

            }
        });


        return view;
    }




    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }


    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     *
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     *
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}
