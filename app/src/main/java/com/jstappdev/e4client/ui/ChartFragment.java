package com.jstappdev.e4client.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SessionData;
import com.jstappdev.e4client.SharedViewModel;
import com.scichart.charting.model.dataSeries.XyDataSeries;
import com.scichart.charting.modifiers.ModifierGroup;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.annotations.HorizontalAnchorPoint;
import com.scichart.charting.visuals.annotations.TextAnnotation;
import com.scichart.charting.visuals.annotations.VerticalAnchorPoint;
import com.scichart.charting.visuals.annotations.VerticalLineAnnotation;
import com.scichart.charting.visuals.axes.IAxis;
import com.scichart.charting.visuals.pointmarkers.EllipsePointMarker;
import com.scichart.charting.visuals.renderableSeries.IRenderableSeries;
import com.scichart.charting.visuals.synchronization.SciChartVerticalGroup;
import com.scichart.data.model.DoubleRange;
import com.scichart.drawing.utility.ColorUtil;
import com.scichart.extensions.builders.SciChartBuilder;

import java.util.Collections;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChartFragment extends Fragment {

    private final SciChartVerticalGroup verticalGroup = new SciChartVerticalGroup();
    private final DoubleRange sharedXRange = new DoubleRange();

    @BindView(R.id.edaChart)
    SciChartSurface edaChart;

    @BindView(R.id.hrChart)
    SciChartSurface hrChart;

    @BindView(R.id.tempChart)
    SciChartSurface tempChart;

    @BindView(R.id.bvpChart)
    SciChartSurface bvpChart;

    private SessionData sessionData;
    private SharedViewModel sharedViewModel;
    private SciChartBuilder sciChartBuilder;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_chart, container, false);

        ButterKnife.bind(this, root);

        SciChartBuilder.init(getActivity());
        sciChartBuilder = SciChartBuilder.instance();

        return root;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupCharts();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SciChartBuilder.dispose();
    }


    private void setupCharts() {
        sessionData = SessionData.getInstance();
        final LifecycleOwner owner = getViewLifecycleOwner();

        // Create a numeric X axis
        final IAxis xAxis = sciChartBuilder.newNumericAxis()
                .withAxisTitle("Time (milliseconds)")
                .withVisibleRange(0, 15)
                .build();

        // Create a numeric Y axis
        final IAxis yAxis = sciChartBuilder.newNumericAxis()
                .withAxisTitle("EDA uS").withVisibleRange(0, 6).build();

        // Create a TextAnnotation and specify the inscription and position for it
        TextAnnotation textAnnotation = sciChartBuilder.newTextAnnotation()
                .withX1(5.0)
                .withY1(55.0)
                .withText("Electrodermal Activity")
                .withHorizontalAnchorPoint(HorizontalAnchorPoint.Center)
                .withVerticalAnchorPoint(VerticalAnchorPoint.Center)
                .withFontStyle(20, ColorUtil.White)
                .build();

        // Create interactivity modifiers
        ModifierGroup chartModifiers = sciChartBuilder.newModifierGroup()
                //.withPinchZoomModifier().withReceiveHandledEvents(true).build()
                //.withZoomPanModifier().withReceiveHandledEvents(true).build()
                //.withXAxisDragModifier().withReceiveHandledEvents(true).withDragMode(AxisDragModifierBase.AxisDragMode.Scale).withClipModeX(ClipMode.None).build()
                //.withYAxisDragModifier().withReceiveHandledEvents(true).withDragMode(AxisDragModifierBase.AxisDragMode.Pan).build()
                .build();

        // Add the Y axis to the YAxes collection of the surface
        Collections.addAll(edaChart.getYAxes(), yAxis);

        // Add the X axis to the XAxes collection of the surface
        Collections.addAll(edaChart.getXAxes(), xAxis);

        // Add the annotation to the Annotations collection of the surface
        Collections.addAll(edaChart.getAnnotations(), textAnnotation);

        // Add the interactions to the ChartModifiers collection of the surface
        Collections.addAll(edaChart.getChartModifiers(), chartModifiers);

        // Create a couple of DataSeries for numeric (Int, Double) data
        final XyDataSeries<Double, Float> edaLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        final XyDataSeries<Double, Float> tagScatterData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();

        // may throw ConcurrentModificationException
        //  todo: add collected data before accepting realtime data
        // lineData.append(sessionData.getGsrTimestamps(), sessionData.getGsr());

        // Create and configure a line series
        final IRenderableSeries lineSeries = sciChartBuilder.newLineSeries()
                .withDataSeries(edaLineData)
                .withStrokeStyle(ColorUtil.LightBlue, 2f, true)
                .build();

        // Create an Ellipse PointMarker for the Scatter Series
        EllipsePointMarker pointMarker = sciChartBuilder
                .newPointMarker(new EllipsePointMarker())
                .withFill(ColorUtil.LightBlue)
                .withStroke(ColorUtil.Green, 2f)
                .withSize(10)
                .build();

        // Create and configure a scatter series
        final IRenderableSeries scatterSeries = sciChartBuilder.newScatterSeries()
                .withDataSeries(tagScatterData)
                .withPointMarker(pointMarker)
                .build();

        // Add a RenderableSeries onto the SciChartSurface
        edaChart.getRenderableSeries().add(scatterSeries);
        edaChart.getRenderableSeries().add(lineSeries);


        sharedViewModel.getLastGsr().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastGsr) {
                edaLineData.append(sessionData.getGsrTimestamps().getLast(), sessionData.getGsr().get(lastGsr));
                edaChart.zoomExtents();
            }
        });
        sharedViewModel.getTag().observe(owner, new Observer<Double>() {
            @Override
            public void onChanged(Double tag) {
                tagScatterData.append(tag, 0f);

                Log.d("e4", "created tag at " + tag);

                // todo: vertical line is not visible
                VerticalLineAnnotation verticalLine = sciChartBuilder.newVerticalLineAnnotation()
                        .withPosition(tag, 0.5d)
                        .withStroke(2, ColorUtil.Orange)
                        .withVerticalGravity(Gravity.FILL_VERTICAL)
                        .withXAxisId("Top_X_Axis")
                        .withYAxisId("Left_Y_Axis")
                        .withIsEditable(false)
                        .build();

                Collections.addAll(edaChart.getAnnotations(), verticalLine);
            }
        });

    }

}