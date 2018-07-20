/*******************************************************************************
 * Copyright (c) 2015-2018 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.deeplearning4j.nn.conf.layers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.deeplearning4j.nn.conf.InputPreProcessor;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.deeplearning4j.util.ConvolutionUtils;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Collection;
import java.util.Map;

import static org.deeplearning4j.nn.conf.layers.InputTypeUtil.getOutputTypeCnnLayers;

/**
 * 1D (temporal) convolutional layer. Currently, we just subclass off the
 * ConvolutionLayer and hard code the "width" dimension to 1. Also, this
 * layer accepts RNN InputTypes instead of CNN InputTypes.
 * <p>
 * This approach treats a multivariate time series with L timesteps and
 * P variables as an L x 1 x P image (L rows high, 1 column wide, P
 * channels deep). The kernel should be H<L pixels high and W=1 pixels
 * wide.
 * <p>
 * TODO: We will eventually want to NOT subclass off of ConvolutionLayer.
 *
 * @author dave@skymind.io
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Convolution1DLayer extends ConvolutionLayer {

    private Convolution1DLayer(Builder builder) {
        super(builder);
        initializeConstraints(builder);
    }

    @Override
    public org.deeplearning4j.nn.api.Layer instantiate(NeuralNetConfiguration conf,
                                                       Collection<TrainingListener> trainingListeners, int layerIndex, INDArray layerParamsView,
                                                       boolean initializeParams) {
        LayerValidation.assertNInNOutSet("Convolution1DLayer", getLayerName(), layerIndex,
                getNIn(), getNOut());

        org.deeplearning4j.nn.layers.convolution.Convolution1DLayer ret =
                new org.deeplearning4j.nn.layers.convolution.Convolution1DLayer(conf);
        ret.setListeners(trainingListeners);
        ret.setIndex(layerIndex);
        ret.setParamsViewArray(layerParamsView);
        Map<String, INDArray> paramTable = initializer().init(conf, layerParamsView, initializeParams);
        ret.setParamTable(paramTable);
        ret.setConf(conf);
        return ret;
    }

    @Override
    public InputType getOutputType(int layerIndex, InputType inputType) {
        if (inputType == null || inputType.getType() != InputType.Type.RNN) {
            throw new IllegalStateException("Invalid input for 1D CNN layer (layer index = " + layerIndex
                    + ", layer name = \"" + getLayerName() + "\"): expect RNN input type with size > 0. Got: "
                    + inputType);
        }
        long inputTsLength = ((InputType.InputTypeRecurrent) inputType).getTimeSeriesLength();
        InputType dummyConv = new InputType.InputTypeConvolutional(inputTsLength, inputTsLength, nOut);
        InputType.InputTypeConvolutional returnConv = (InputType.InputTypeConvolutional)
                InputTypeUtil.getOutputTypeCnnLayers(dummyConv, kernelSize, stride, padding, dilation,
                        convolutionMode, nOut, layerIndex, getLayerName(), ConvolutionLayer.class);
        long outputTsLength = returnConv.getHeight();
        return InputType.recurrent(nOut, outputTsLength);

    }

    @Override
    public void setNIn(InputType inputType, boolean override) {
        if (inputType == null || inputType.getType() != InputType.Type.RNN) {
            throw new IllegalStateException("Invalid input for 1D CNN layer (layer name = \"" + getLayerName()
                    + "\"): expect RNN input type with size > 0. Got: " + inputType);
        }

        if (nIn <= 0 || override) {
            InputType.InputTypeRecurrent r = (InputType.InputTypeRecurrent) inputType;
            this.nIn = r.getSize();
        }
    }

    @Override
    public InputPreProcessor getPreProcessorForInputType(InputType inputType) {
        if (inputType == null) {
            throw new IllegalStateException("Invalid input for Convolution1D layer (layer name=\"" + getLayerName()
                    + "\"): input is null");
        }

        return InputTypeUtil.getPreprocessorForInputTypeRnnLayers(inputType, getLayerName());
    }

    public static class Builder extends ConvolutionLayer.BaseConvBuilder<Builder> {

        public Builder() {
            this(0, 1, 0);
            this.kernelSize = null;
        }

        public Builder(int kernelSize, int stride) {
            this(kernelSize, stride, 0);
        }

        public Builder(int kernelSize) {
            this(kernelSize, 1, 0);
        }

        public Builder(int kernelSize, int stride, int padding) {
            this.kernelSize = new int[]{kernelSize, 1};
            this.stride = new int[]{stride, 1};
            this.padding = new int[]{padding, 0};
        }

        /**
         * Size of the convolution
         *
         * @param kernelSize the length of the kernel
         */
        public Builder kernelSize(int kernelSize) {
            this.kernelSize = new int[]{kernelSize, 1};
            return this;
        }

        public Builder stride(int stride) {
            this.stride[0] = stride;
            return this;
        }

        public Builder padding(int padding) {
            this.padding[0] = padding;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Convolution1DLayer build() {
            ConvolutionUtils.validateConvolutionModePadding(convolutionMode, padding);
            ConvolutionUtils.validateCnnKernelStridePadding(kernelSize, stride, padding);

            return new Convolution1DLayer(this);
        }
    }
}
