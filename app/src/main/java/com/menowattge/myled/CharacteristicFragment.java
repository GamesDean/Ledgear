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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

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



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


   /// protected static ToggleButton disConnect;

    protected static CircularPulsingButton pulseButtonConnect;
  //  protected static CircularPulsingButton fabButtonDisconnect;
  protected static FabButton fabButtonDisconnect;


    private OnFragmentInteractionListener mListener;
    protected Spinner spinnerDue ;
    protected ArrayAdapter adapter;
    //protected Button invia;
    protected  FabButton invia;
    protected TextView textView;
    protected TextView textViewFive;

    protected String selezionaProgramma = "----------> seleziona un programma <----------";
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

    //dichiarato per essere instanziato nel main
    public void showUuid(UUID a){
        Toast.makeText(getActivity(),"->Device Connesso<-"+"\n  UUID : "+a,Toast.LENGTH_LONG).show();
        Toast.makeText(getActivity(),"seleziona un programma",Toast.LENGTH_LONG).show();


        spinnerDue.postDelayed(new Runnable() {
            @Override
            public void run() {
                // ad avvenuta connessione spinner fade in
                final Animation anim = android.view.animation.AnimationUtils.loadAnimation(spinnerDue.getContext(), android.R.anim.fade_in);
                spinnerDue.setAnimation(anim);
                spinnerDue.setVisibility(View.VISIBLE);
            }
        },2100);

    }

    public CharacteristicFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CharacteristicFragment.
     */
    // TODO: Rename and change types and number of parameters
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

     View view = null;


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {


        view = inflater.inflate(R.layout.fragment_characteristic, container, false);

        invia = (FabButton)view.findViewById(R.id.buttonInvia);
        //testo sopra i pulsanti "connect" "disconnect", obbligatorio che i FAB non hanno il testo
        textView =(TextView)view.findViewById(R.id.textView2);
        textViewFive =(TextView)view.findViewById(R.id.textView5);

        /// disConnect = (ToggleButton) view.findViewById(R.id.toggleButton);
        pulseButtonConnect=(CircularPulsingButton) view.findViewById(R.id.pulsebuttonConnect);
        //fabButtonDisconnect=(CircularPulsingButton) view.findViewById(R.id.fabButtonDisconnect);
        fabButtonDisconnect=(FabButton)view.findViewById(R.id.fabButtonDisconnect);
        fabButtonDisconnect.setVisibility(View.INVISIBLE);

        spinnerDue = (Spinner)view.findViewById(R.id.spinner2);




        adapter = new ArrayAdapter<>(getActivity(),R.layout.spinner_text,//android.R.layout.simple_spinner_dropdown_item,
                new String[]{selezionaProgramma,
                        programmaUno,programmaDue,programmaTre,programmaQuattro,programmaCinque,programmaSei,
                        programmaSette, programmaOtto,programmaNove,programmaDieci,programmaUndici,programmaDodici,
                        programmaTredici, programmaQuattordici,programmaQuindici,programmaSedici,programmaDiciassette,
                        programmaDiciotto,programmaDiciannove});


        spinnerDue.setAdapter(adapter);



        // li rendo invisibili per poi farli riapparire con un'animazione
        spinnerDue.setVisibility(View.INVISIBLE);
        invia.setVisibility(View.INVISIBLE);

        textView.setVisibility(View.INVISIBLE);
        textViewFive.setVisibility(View.INVISIBLE);


        pulseButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((MainActivity) getActivity()).connectToDevice(((MainActivity) getActivity()).deviceAddress);

                pulseButtonConnect.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: magari ci metto un'animazione che lo fa comparire gradualmente e levo setAlpha
                        final Animation animDisLeft = android.view.animation.AnimationUtils.loadAnimation(fabButtonDisconnect.getContext(),  android.R.anim.slide_in_left);
                        final Animation animCon = android.view.animation.AnimationUtils.loadAnimation(fabButtonDisconnect.getContext(),  R.anim.scale_down);

                        pulseButtonConnect.setAnimation(animCon);
                        pulseButtonConnect.setVisibility(View.INVISIBLE);

                        fabButtonDisconnect.setAnimation(animDisLeft);
                        fabButtonDisconnect.setVisibility(View.VISIBLE);

                        textViewFive.setAnimation(animDisLeft);
                        textViewFive.setVisibility(View.VISIBLE);
                    }
                },2000);

            }
        });

        fabButtonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                BluetoothGatt btGatt = ((MainActivity) getActivity()).connectToDevice(((MainActivity) getActivity()).deviceAddress);

                ((MainActivity) getActivity()).discConnectToDevice(btGatt);
                //rimuove il fragment dallo stack come quando si preme il tasto "back"
                getFragmentManager().popBackStack();
                //deve tornare invisibile
                MainActivity.startScanningButton.setVisibility(View.INVISIBLE);
                //deve tornare cliccabile
                MainActivity.startScanningButton.setEnabled(true);
                //riesegue la scansione per ripopolare la lista
                ((MainActivity)getActivity()).startScanning();

            }
        });


        return view;
    }



    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}
