/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "NeuralNetworksWrapper.h"

#include <gtest/gtest.h>

using namespace android::nn::wrapper;

namespace {

typedef float Matrix3x4[3][4];

class TrivialTest : public ::testing::Test {
protected:
    virtual void SetUp() { ASSERT_EQ(Initialize(), Result::NO_ERROR); }
    virtual void TearDown() { Shutdown(); }

    const Matrix3x4 matrix1 = {{1.f, 2.f, 3.f, 4.f}, {5.f, 6.f, 7.f, 8.f}, {9.f, 10.f, 11.f, 12.f}};
    const Matrix3x4 matrix2 = {{100.f, 200.f, 300.f, 400.f},
                               {500.f, 600.f, 700.f, 800.f},
                               {900.f, 1000.f, 1100.f, 1200.f}};
    const Matrix3x4 matrix3 = {{20.f, 30.f, 40.f, 50.f},
                               {21.f, 22.f, 23.f, 24.f},
                               {31.f, 32.f, 33.f, 34.f}};
    const Matrix3x4 expected2 = {{101.f, 202.f, 303.f, 404.f},
                                 {505.f, 606.f, 707.f, 808.f},
                                 {909.f, 1010.f, 1111.f, 1212.f}};
    const Matrix3x4 expected3 = {{121.f, 232.f, 343.f, 454.f},
                                 {526.f, 628.f, 730.f, 832.f},
                                 {940.f, 1042.f, 1144.f, 1246.f}};
    const Matrix3x4 expected3b = {{22.f, 34.f, 46.f, 58.f},
                                  {31.f, 34.f, 37.f, 40.f},
                                  {49.f, 52.f, 55.f, 58.f}};
};

// Create a model that can add two tensors using a one node graph.
void CreateAddTwoTensorModel(Model* model) {
    OperandType matrixType(Type::TENSOR_FLOAT32, {3, 4});
    auto a = model->addOperand(&matrixType);
    auto b = model->addOperand(&matrixType);
    auto c = model->addOperand(&matrixType);
    model->addOperation(ANEURALNETWORKS_ADD, {a, b}, {c});
    model->setInputsAndOutputs({a, b}, {c});
    ASSERT_TRUE(model->isValid());
}

// Create a model that can add three tensors using a two node graph,
// with one tensor set as part of the model.
void CreateAddThreeTensorModel(Model* model, const Matrix3x4 bias) {
    OperandType matrixType(Type::TENSOR_FLOAT32, {3, 4});
    auto a = model->addOperand(&matrixType);
    auto b = model->addOperand(&matrixType);
    auto c = model->addOperand(&matrixType);
    auto d = model->addOperand(&matrixType);
    auto e = model->addOperand(&matrixType);
    model->setOperandValue(e, bias, sizeof(Matrix3x4));
    model->addOperation(ANEURALNETWORKS_ADD, {a, c}, {b});
    model->addOperation(ANEURALNETWORKS_ADD, {b, e}, {d});
    model->setInputsAndOutputs({c, a}, {d});
    ASSERT_TRUE(model->isValid());
}

// Check that the values are the same. This works only if dealing with integer
// value, otherwise we should accept values that are similar if not exact.
int CompareMatrices(const Matrix3x4 expected, const Matrix3x4 actual) {
    int errors = 0;
    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 4; j++) {
            if (expected[i][j] != actual[i][j]) {
                printf("expected[%d][%d] != actual[%d][%d], %f != %f\n", i, j, i, j,
                       static_cast<double>(expected[i][j]), static_cast<double>(actual[i][j]));
                errors++;
            }
        }
    }
    return errors;
}

// TODO convert this test to a gtest format
TEST_F(TrivialTest, AddTwo) {
    Model modelAdd2;
    CreateAddTwoTensorModel(&modelAdd2);

    // Test the one node model.
    Matrix3x4 actual;
    memset(&actual, 0, sizeof(actual));
    Request request(&modelAdd2);
    ASSERT_EQ(request.setInput(0, matrix1, sizeof(Matrix3x4)), Result::NO_ERROR);
    ASSERT_EQ(request.setInput(1, matrix2, sizeof(Matrix3x4)), Result::NO_ERROR);
    ASSERT_EQ(request.setOutput(0, actual, sizeof(Matrix3x4)), Result::NO_ERROR);
    ASSERT_EQ(request.compute(), Result::NO_ERROR);
    ASSERT_EQ(CompareMatrices(expected2, actual), 0);
}

TEST_F(TrivialTest, AddThree) {
    Model modelAdd3;
    CreateAddThreeTensorModel(&modelAdd3, matrix3);

    // Test the two node model.
    Matrix3x4 actual;
    memset(&actual, 0, sizeof(actual));
    Request request2(&modelAdd3);
    ASSERT_EQ(request2.setInput(0, matrix1, sizeof(Matrix3x4)), Result::NO_ERROR);
    ASSERT_EQ(request2.setInput(1, matrix2, sizeof(Matrix3x4)), Result::NO_ERROR);
    ASSERT_EQ(request2.setOutput(0, actual, sizeof(Matrix3x4)), Result::NO_ERROR);
    ASSERT_EQ(request2.compute(), Result::NO_ERROR);
    ASSERT_EQ(CompareMatrices(expected3, actual), 0);

    // Test it a second time to make sure the model is reusable.
    memset(&actual, 0, sizeof(actual));
    Request request3(&modelAdd3);
    ASSERT_EQ(request3.setInput(0, matrix1, sizeof(Matrix3x4)), Result::NO_ERROR);
    ASSERT_EQ(request3.setInput(1, matrix1, sizeof(Matrix3x4)), Result::NO_ERROR);
    ASSERT_EQ(request3.setOutput(0, actual, sizeof(Matrix3x4)), Result::NO_ERROR);
    ASSERT_EQ(request3.compute(), Result::NO_ERROR);
    ASSERT_EQ(CompareMatrices(expected3b, actual), 0);
}

}  // end namespace
