class Matrice:
    def __init__(self, n_init):
        self.n = n_init
        self.C = []
        self.I = []
        self.L = [int(0)] * (n_init+1)

    def __str__(self):
        return "Matrice({})\nC: {}\nI: {}\nL: {}\n".format(self.n, self.C, self.I, self.L)

    def set(self, i, j, value):
        """
        Insert a new value at position (i,j)
        :param i: position in line
        :param j: position in column
        """
        start = self.L[i]
        end = self.L[i+1]
        pos = -1
        for p in range(start, end):
            if self.I[p] == j:
                self.C[p] = float(value)
                return
            if self.I[p] > j:
                pos = p
                break
        if pos == -1:
            pos = end
        self.I.insert(pos, int(j))
        self.C.insert(pos, round(float(value), 3))
        for p in range(i+1, self.n+1):
            self.L[p] = self.L[p] + 1
        return

    def get(self, i, j):
        """
        Return the value at position (i,j)
        :param i: number in line
        :param j: number in column
        :return: the value at that position
        """
        start = self.L[i]
        end = self.L[i+1]
        for p in range(start, end):
            if self.I[p] == j:
                return self.C[p]
            elif self.I[p] > j:
                return 0
            else:
                continue
        return 0

    def equals(self, matrice):
        if self.n != matrice.n:
            #print("Diff length")
            return False
        #print("Check C")
        for i in range(len(self.C)):
            if self.C[i] != matrice.C[i]:
                #print("Diff C", i)
                return False
        #print("Check L")
        for i in range(len(self.L)):
            if self.L[i] != matrice.L[i]:
                #print("Diff L", i)
                return False
        #print("Check I")
        for i in range(len(self.I)):
            if self.I[i] != matrice.I[i]:
                #print("Diff I : ", i, type(self.I[i]), "!=", type(matrice.I[i]))
                return False
        #print("Done")
        return True

    def multiply_opt(self, v):
        res = Vecteur(self.n)
        for i in range(0, self.n):
            for j in range(self.L[i], self.L[i+1]):
                res.L[i] += self.C[j] * v.get(self.I[j])
        return res

    def compute_transp(self, pi):
        """
        Compute a transpose CLI Matrix times a vector
        :param self: the matrix
        :param pi: the vector
        :return: p a new vector
        """
        p = Vecteur(self.n)
        s = 0
        for i in range(0, self.n):
            if self.L[i] == self.L[i+1]:
                s += pi[i]
            else:
                for j in range(self.L[i], self.L[i+1]):
                    p.set(self.I[j], p[self.I[j]] + self.C[j] * pi[i])
        s = float(s/self.n)
        for i in range(0, self.n):
            p.set(i, p[i] + s)
        return p

    def pagerank(self,k):
        """
        Compute the page rank on a CLI matrix
        :param self: the matrix
        :param k: the number of iteration
        :return: pi, the vector of the page rank
        """
        alpha = 0.15
        pi = Vecteur(self.n , 1 / self.n)
        J = Vecteur(self.n, alpha / self.n)
        for _ in range(0,k):
            pi = self.compute_transp(pi)
            pi.multiply_by_factor(1 - alpha)
        #print("\n")
        return pi


    @staticmethod
    def get_example():
        example = Matrice(4)
        example.set(0, 2, 1)
        example.set(1,0,2)
        example.set(1,1,3)
        example.set(1,3,4)
        example.set(2,1,5)
        example.set(2,2,6)
        example.set(2,3,7)
        return example

    @staticmethod
    def get_example_tp():
        example = Matrice(4)
        example.set(0, 1, 3)
        example.set(0, 2, 5)
        example.set(0, 3, 8)

        example.set(1, 0, 1)
        example.set(1, 2, 2)

        example.set(3, 1, 3)
        return example

    @staticmethod
    def build_from(links):
        """
        Create a CLI matrix by running on an link index
        :param links:
        :return: a cli matrix
        """
        #print("Build CLI:", end="")
        size = len(links)
        m = Matrice(size)
        sum = 0
        for id, pages in links.items():
            deg = len(pages)
            sum += deg
            for _ in range(deg):
                m.C.append(round(float(1/deg), 3))
            m.L[id+1] = int(m.L[id]+deg)
            for elt in pages:
                m.I.append(int(elt))
        #print("DONE->{}".format(sum))
        #print(len(m.L), len(m.C), len(m.I))
        return m

    def write_matrix(self, file):
        """
        Write the matric sef on the disk
        """
        with open(file, "a") as fd:
            fd.write(str(self.n) + '\n')
            for i in range(0, len(self.C)):
                fd.write(str(self.C[i]) + ' ')
            fd.write('\n')
            for i in range(0, len(self.I)):
                fd.write(str(self.I[i]) + ' ')
            fd.write('\n')
            for i in range(0, len(self.L)):
                fd.write(str(self.L[i]) + ' ')
        return

    @staticmethod
    def read_matrix(file):
        """
        Get a Matrix from a file
        """
        #print("Load Matrix: ", end="")
        with open(file, "r") as fd:
            g = fd.read().split("\n")
        m = Matrice(int(g[0]))
        lis_C = list(g[1].split())
        lis_I = list(g[2].split())
        lis_L = list(g[3].split())
        for e in lis_C:
            m.C.append(float(e))
        for e in lis_I:
            m.I.append(int(e))
        for i in range(0, len(lis_L)):
            m.L[i] = int(lis_L[i])
        #print("DONE")
        return m

    def exec_and_store_pagerank(self, k, file):
        """
        Compute and write the page rank in a file
        :param k: the number of iteration
        :param file: file where to store
        """
        #print("Start page rank")
        pi = self.pagerank(k)
        pi.write_vector(file)
        #print("Page rank: DONE")
        return None

    @staticmethod
    def get_pagerank(file):
        """
        Get page rank as dictionnary
        """
        dic = dict()
        index = 0

        with open(file, 'r') as fd:
            for rank in fd:
                dic[index] = float(rank)
                index += 1
            return dic
        return None

class Vecteur:
    def __init__(self, size, default=0):
        self.n = size
        self.L = [default] * size

    def __str__(self):
        return "Vecteur: {}\n".format(self.L)

    def __iter__(self):
        self.i = 0
        return iter(self.L)

    def __next__(self):
        if self.i <= self.n:
            res = self.L[self.i]
            self.i += 1
            return res
        else:
            raise StopIteration

    def get(self, i):
        return self.L[i]

    def __getitem__(self, key):
        return self.L[key]

    def set(self, i, value):
        self.L[i] = value
        return

    def equals(self, vecteur):
        if self.n != vecteur.n:
            return False

        for x,y in zip(self.L, vecteur.L):
            if x != y:
                return False

        return True

    def multiply_by_factor(self, factor):
        """
        Multiply in place the current vector
        :param self: the new vector
        :param factor: the new factor
        :return: None
        """
        for i in range(0, self.n):
            self.L[i] *= factor
        return self


    def sum_with(self,v):
        """
        Sum the current vector with another vector
        :param self: the vector
        :param v: the vector to sum with
        :return: True in cas of succeed
        """
        if self.n != v.n:
            return None
        else:
            for i in range(0, self.n):
                self.L[i] += v.L[i]
            return self

    def write_vector(self, file):
        """
        Write a vector into a file
        :param file: the file where to store
        """
        with open(file, "a") as fd:
            for i in range(self.n):
                fd.write(str(self[i]) + '\n')
        return None

    @staticmethod
    def read_vector(file):
        """
        Read a vector from a file
        :param file: where to read

        """
        with open(file, "r") as fd:
            g = fd.read().split("\n")
            v = Vecteur(int(g[0]))
            values = g[1].split()
            for i in range(0, v.n):
                v.set(i, values[i])
            return v
        return None

def test_cli_from_link(link_path):
    _cli = Matrice.build_from(link_path)
    #print(cli)
    return None



def test_vecteur():
    def test_sum():
        v = Vecteur(10, default=10)

        # Add Vector {0, 0, ... }
        if not v.sum_with(Vecteur(10, default=0)):
            print("Invalid size")
        for e in v:
            if e != 10:
                print("Invalid sum with 0")

        # Add Vector {-10, -10, ... }
        if not v.sum_with(Vecteur(10, default=-10)):
            print("Invalid size")
        for e in v:
            if e != 0:
                print("Invalid sum with -10")

        if v.sum_with(Vecteur(5, default=0)):
            print("Should fail on different size")

        print("Done test_sum()")

    def test_multiply():
        v = Vecteur(10, default=10)

        # Multiply Vector 1
        v.multiply_by_factor(1)
        for e in v:
            if e != 10:
                print("Invalid multiply with 1")

        # Multiply Vector 0
        v.multiply_by_factor(0)
        for e in v:
            if e != 0:
                print("Invalid multiply with 0")

        print("Done test_multiply()")

    test_sum()
    test_multiply()

def test_matrice():
    def test_multiply_with_vect():
        matrice = Matrice.get_example_tp()

        # Multiply with 1
        v = Vecteur(4, default=1)
        res = matrice.multiply_opt(v)
        supposed_res = Vecteur(4)
        supposed_res.set(0, 16)
        supposed_res.set(1, 3)
        supposed_res.set(2, 0)
        supposed_res.set(3, 3)

        if not supposed_res.equals(res):
            print("Invalid multiply0 witch vect {1,1,1,1}")

        # Multiply with {4, 6, 3, 12}
        v = Vecteur(4, default=1)
        v.set(0, 4)
        v.set(1, 6)
        v.set(2, 3)
        v.set(3, 12)
        res = matrice.multiply_opt(v)
        supposed_res = Vecteur(4)
        supposed_res.set(0, 129)
        supposed_res.set(1, 10)
        supposed_res.set(2, 0)
        supposed_res.set(3, 18)

        if not supposed_res.equals(res):
            print("Invalid multiply with {4,6,3,12}")

        print("Done test_multiply_with_vect()")

    def test_compute_transp():
        matrice = Matrice.get_example_tp()

        # Compute_transpose of matrice and {1, 1, 1, 1}
        pi = Vecteur(4, default=1)
        res = matrice.compute_transp(pi)
        supposed_res = Vecteur(4)
        supposed_res.set(0, 1.25)
        supposed_res.set(1, 6.25)
        supposed_res.set(2, 7.25)
        supposed_res.set(3, 8.25)

        if not supposed_res.equals(res):
            print("Invalid compute transp with {1, 1, 1, 1}")

        print("Done test_compute_transp()")
        for i in range (4):
            print(res[i])

    test_multiply_with_vect()
    test_compute_transp()
